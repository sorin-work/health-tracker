package com.healthtracker.report;

import com.healthtracker.activity.ActivityService;
import com.healthtracker.activity.dto.ActivityResponse;
import com.healthtracker.common.ApiException;
import com.healthtracker.journal.JournalService;
import com.healthtracker.medication.DoseService;
import com.healthtracker.medication.DoseStatus;
import com.healthtracker.medication.MedicationService;
import com.healthtracker.medication.ScheduleType;
import com.healthtracker.medication.dto.AdherenceResponse;
import com.healthtracker.medication.dto.AdherenceResponse.MedicationAdherence;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.mood.MoodService;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.report.DoctorReport.ActivityStats;
import com.healthtracker.report.DoctorReport.MedicationLine;
import com.healthtracker.report.DoctorReport.MoodStats;
import com.healthtracker.report.DoctorReport.SymptomStats;
import com.healthtracker.report.DoctorReport.VitalStats;
import com.healthtracker.symptom.SymptomService;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.User;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalService;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pulls the numbers for the printable report together from the other features. */
@Service
public class ReportService {

    static final int MAX_DAYS = 366;
    static final int MAX_FLAGGED = 30;

    private final UserService userService;
    private final VitalService vitalService;
    private final MedicationService medicationService;
    private final DoseService doseService;
    private final SymptomService symptomService;
    private final MoodService moodService;
    private final ActivityService activityService;
    private final JournalService journalService;
    private final Clock clock;

    public ReportService(
            UserService userService,
            VitalService vitalService,
            MedicationService medicationService,
            DoseService doseService,
            SymptomService symptomService,
            MoodService moodService,
            ActivityService activityService,
            JournalService journalService,
            Clock clock) {
        this.userService = userService;
        this.vitalService = vitalService;
        this.medicationService = medicationService;
        this.doseService = doseService;
        this.symptomService = symptomService;
        this.moodService = moodService;
        this.activityService = activityService;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DoctorReport report(Long userId, LocalDate from, LocalDate to) {
        User user = userService.get(userId);
        ZoneId zone = user.zone();
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(zone));
        LocalDate start = from != null ? from : end.minusDays(89);
        if (end.isBefore(start)) {
            throw ApiException.badRequest("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw ApiException.badRequest("Pick a period of at most " + MAX_DAYS + " days");
        }

        List<VitalReadingResponse> readings = vitalService.readings(userId, null, start, end, false);
        List<VitalReadingResponse> flagged = readings.stream()
                .filter(r -> r.flag() != null && r.flag().outOfRange())
                .toList();
        AdherenceResponse adherence = doseService.adherence(userId, start, end);

        return new DoctorReport(
                user.getDisplayName(),
                user.getTimezone(),
                start,
                end,
                clock.instant(),
                vitalStats(readings),
                flagged.stream().limit(MAX_FLAGGED).toList(),
                flagged.size(),
                adherence.overall(),
                medications(userId, start, end, adherence),
                symptomStats(symptomService.entries(userId, start, end, null)),
                moodStats(moodService.entries(userId, start, end)),
                activityStats(activityService.entries(userId, start, end, null), start, end),
                journalService.entries(userId, start, end, null).reversed());
    }

    static List<VitalStats> vitalStats(List<VitalReadingResponse> readings) {
        Map<VitalType, List<VitalReadingResponse>> byType = readings.stream()
                .collect(Collectors.groupingBy(VitalReadingResponse::type, TreeMap::new, Collectors.toList()));
        List<VitalStats> stats = new ArrayList<>();
        byType.forEach((type, list) -> {
            List<BigDecimal> values = list.stream().map(VitalReadingResponse::value).toList();
            List<BigDecimal> secondary = list.stream()
                    .map(VitalReadingResponse::secondaryValue)
                    .filter(Objects::nonNull)
                    .toList();
            stats.add(new VitalStats(
                    type,
                    type.unit(),
                    list.size(),
                    average(values),
                    average(secondary),
                    values.stream().min(BigDecimal::compareTo).orElse(null),
                    values.stream().max(BigDecimal::compareTo).orElse(null),
                    (int) list.stream().filter(r -> r.flag() != null && r.flag().outOfRange()).count()));
        });
        return stats;
    }

    /** Medications taken at any point in the period, with their adherence (or how often an as-needed one was taken). */
    private List<MedicationLine> medications(Long userId, LocalDate start, LocalDate end, AdherenceResponse adherence) {
        Map<Long, MedicationAdherence> byId = adherence.medications().stream()
                .collect(Collectors.toMap(MedicationAdherence::medicationId, Function.identity()));
        Map<Long, Long> asNeededTaken = doseService.history(userId, start, end, false).stream()
                .filter(d -> d.asNeeded() && d.status() == DoseStatus.TAKEN)
                .collect(Collectors.groupingBy(DoseResponse::medicationId, Collectors.counting()));
        return medicationService.list(userId, true).stream()
                .filter(m -> !m.startDate().isAfter(end) && (m.endDate() == null || !m.endDate().isBefore(start)))
                .map(m -> new MedicationLine(
                        m.name(),
                        m.dosage(),
                        m.asNeeded() ? "AS_NEEDED" : (m.scheduleType() == null ? ScheduleType.DAILY : m.scheduleType()).name(),
                        m.times(),
                        m.intervalDays(),
                        m.daysOfWeek(),
                        m.startDate(),
                        m.endDate(),
                        m.asNeeded(),
                        byId.containsKey(m.id()) ? byId.get(m.id()).stats() : null,
                        asNeededTaken.getOrDefault(m.id(), 0L).intValue()))
                .sorted(Comparator.comparing(MedicationLine::asNeeded).thenComparing(MedicationLine::name))
                .toList();
    }

    static List<SymptomStats> symptomStats(List<SymptomResponse> symptoms) {
        Map<String, List<SymptomResponse>> byName = symptoms.stream()
                .collect(Collectors.groupingBy(SymptomResponse::name));
        return byName.entrySet().stream()
                .map(e -> new SymptomStats(
                        e.getKey(),
                        e.getValue().size(),
                        average(e.getValue().stream().map(s -> BigDecimal.valueOf(s.severity())).toList()),
                        e.getValue().stream().mapToInt(SymptomResponse::severity).max().orElse(0)))
                .sorted(Comparator.comparingInt(SymptomStats::count).reversed().thenComparing(SymptomStats::name))
                .toList();
    }

    static MoodStats moodStats(List<MoodResponse> entries) {
        return new MoodStats(
                entries.size(),
                average(entries.stream().map(m -> BigDecimal.valueOf(m.mood())).toList()),
                average(entries.stream()
                        .map(MoodResponse::energy)
                        .filter(Objects::nonNull)
                        .map(BigDecimal::valueOf)
                        .toList()));
    }

    static ActivityStats activityStats(List<ActivityResponse> activities, LocalDate start, LocalDate end) {
        int minutes = activities.stream().mapToInt(ActivityResponse::durationMinutes).sum();
        BigDecimal weeks = BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1)
                .divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);
        return new ActivityStats(
                activities.size(), minutes, BigDecimal.valueOf(minutes).divide(weeks, 0, RoundingMode.HALF_UP));
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }
}
