package com.healthtracker.medication;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.RecordDoseRequest;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoseService {

    static final int MAX_RANGE_DAYS = 366;

    private final MedicationRepository medications;
    private final DoseLogRepository doseLogs;
    private final UserService userService;
    private final Clock clock;

    public DoseService(
            MedicationRepository medications, DoseLogRepository doseLogs, UserService userService, Clock clock) {
        this.medications = medications;
        this.doseLogs = doseLogs;
        this.userService = userService;
        this.clock = clock;
    }

    /** The day's schedule: every due dose plus any as-needed doses that were taken. */
    @Transactional(readOnly = true)
    public List<DoseResponse> forDay(Long userId, LocalDate date) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate day = date != null ? date : today(zone);
        return history(userId, day, day, zone, true);
    }

    /** All doses in a range, oldest first. Pending doses are optional (they are noise in exports). */
    @Transactional(readOnly = true)
    public List<DoseResponse> history(Long userId, LocalDate from, LocalDate to, boolean includePending) {
        return history(userId, from, to, userService.zoneOf(userId), includePending);
    }

    @Transactional
    public DoseResponse record(Long userId, Long medicationId, RecordDoseRequest request) {
        Medication medication = medications.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> ApiException.notFound("Medication"));
        ZoneId zone = userService.zoneOf(userId);
        LocalDate day = LocalDate.ofInstant(request.scheduledAt(), zone);

        if (request.status() == DoseStatus.MISSED || request.status() == DoseStatus.PENDING) {
            throw ApiException.badRequest("A dose can only be logged as taken or skipped");
        }
        if (medication.isAsNeeded()) {
            if (request.status() != DoseStatus.TAKEN) {
                throw ApiException.badRequest("As-needed doses can only be logged as taken");
            }
            if (request.scheduledAt().isAfter(clock.instant().plusSeconds(600))) {
                throw ApiException.badRequest("Doses cannot be logged in the future");
            }
        } else if (!DoseSchedule.slotsOn(medication, day, zone).contains(request.scheduledAt())) {
            throw ApiException.badRequest("No dose of " + medication.getName() + " is scheduled at that time");
        }

        DoseLog log = doseLogs.findByMedicationIdAndScheduledAt(medicationId, request.scheduledAt())
                .orElseGet(() -> new DoseLog(medicationId, userId, request.scheduledAt()));
        log.record(request.status(), clock.instant(), trimToNull(request.note()));
        DoseLog saved = doseLogs.save(log);
        return DoseResponse.of(medication, saved.getScheduledAt(), saved.getStatus(), saved);
    }

    @Transactional
    public void undo(Long userId, Long logId) {
        DoseLog log = doseLogs.findByIdAndUserId(logId, userId).orElseThrow(() -> ApiException.notFound("Dose"));
        doseLogs.delete(log);
    }

    private List<DoseResponse> history(
            Long userId, LocalDate from, LocalDate to, ZoneId zone, boolean includePending) {
        checkRange(from, to);
        List<Medication> meds = medications.findByUserIdOrderByNameAsc(userId);
        DateRange range = DateRange.of(from, to, zone);
        List<DoseLog> logs = doseLogs.findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
                userId, range.start(), range.end());

        List<DoseResponse> result = new ArrayList<>();
        for (ScheduledDose dose : DoseSchedule.expand(meds, logs, from, to, zone, today(zone))) {
            if (includePending || dose.status() != DoseStatus.PENDING) {
                result.add(DoseResponse.from(dose));
            }
        }
        Map<Long, Medication> asNeeded = meds.stream()
                .filter(Medication::isAsNeeded)
                .collect(Collectors.toMap(Medication::getId, Function.identity()));
        for (DoseLog log : logs) {
            Medication m = asNeeded.get(log.getMedicationId());
            if (m != null) {
                result.add(DoseResponse.of(m, log.getScheduledAt(), log.getStatus(), log));
            }
        }
        result.sort(Comparator.comparing(DoseResponse::scheduledAt).thenComparing(DoseResponse::medicationName));
        return result;
    }

    private LocalDate today(ZoneId zone) {
        return LocalDate.now(clock.withZone(zone));
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw ApiException.badRequest("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("Pick a range of at most " + MAX_RANGE_DAYS + " days");
        }
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
