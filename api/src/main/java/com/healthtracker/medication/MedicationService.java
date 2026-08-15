package com.healthtracker.medication;

import com.healthtracker.common.ApiException;
import com.healthtracker.medication.dto.MedicationRequest;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationService {

    private final MedicationRepository medications;
    private final UserService userService;
    private final Clock clock;

    public MedicationService(MedicationRepository medications, UserService userService, Clock clock) {
        this.medications = medications;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> list(Long userId, boolean includeArchived) {
        return medications.findByUserIdOrderByNameAsc(userId).stream()
                .filter(m -> includeArchived || m.isActive())
                .map(MedicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicationResponse get(Long userId, Long id) {
        return MedicationResponse.from(find(userId, id));
    }

    @Transactional
    public MedicationResponse create(Long userId, MedicationRequest request) {
        Medication medication = new Medication(userId, request.name().trim(), clock.instant());
        apply(medication, request);
        return MedicationResponse.from(medications.save(medication));
    }

    @Transactional
    public MedicationResponse update(Long userId, Long id, MedicationRequest request) {
        Medication medication = find(userId, id);
        apply(medication, request);
        return MedicationResponse.from(medication);
    }

    /** Stops a medication: it disappears from today's schedule but its history stays. */
    @Transactional
    public MedicationResponse archive(Long userId, Long id) {
        Medication medication = find(userId, id);
        LocalDate today = LocalDate.now(clock.withZone(userService.zoneOf(userId)));
        medication.setActive(false);
        if (medication.getEndDate() == null || medication.getEndDate().isAfter(today)) {
            medication.setEndDate(today.isBefore(medication.getStartDate()) ? medication.getStartDate() : today);
        }
        return MedicationResponse.from(medication);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        medications.delete(find(userId, id));
    }

    Medication find(Long userId, Long id) {
        return medications.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Medication"));
    }

    private static void apply(Medication medication, MedicationRequest request) {
        List<LocalTime> times = request.times() == null
                ? List.of()
                : request.times().stream().map(t -> t.withSecond(0).withNano(0)).distinct().sorted().toList();
        if (!request.asNeeded() && times.isEmpty()) {
            throw ApiException.badRequest("Add at least one time of day, or mark the medication as needed");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("End date must not be before the start date");
        }
        ScheduleType schedule = request.asNeeded() || request.scheduleType() == null
                ? ScheduleType.DAILY
                : request.scheduleType();
        if (schedule == ScheduleType.EVERY_N_DAYS && request.intervalDays() == null) {
            throw ApiException.badRequest("Say how many days apart the doses are");
        }
        if (schedule == ScheduleType.WEEKLY && (request.daysOfWeek() == null || request.daysOfWeek().isEmpty())) {
            throw ApiException.badRequest("Pick at least one day of the week");
        }
        medication.setName(request.name().trim());
        medication.setDosage(trimToNull(request.dosage()));
        medication.setInstructions(trimToNull(request.instructions()));
        medication.setAsNeeded(request.asNeeded());
        medication.setTimes(request.asNeeded() ? List.of() : times);
        medication.setStartDate(request.startDate());
        medication.setEndDate(request.endDate());
        medication.setScheduleType(schedule);
        medication.setIntervalDays(schedule == ScheduleType.EVERY_N_DAYS ? request.intervalDays() : null);
        medication.setDaysOfWeek(schedule == ScheduleType.WEEKLY ? request.daysOfWeek() : null);
        medication.setSupply(request.supply());
        medication.setDoseQuantity(request.doseQuantity() != null ? request.doseQuantity() : BigDecimal.ONE);
        medication.setRefillWarningDays(request.refillWarningDays() != null ? request.refillWarningDays() : 7);
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
