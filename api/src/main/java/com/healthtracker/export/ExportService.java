package com.healthtracker.export;

import com.healthtracker.activity.ActivityService;
import com.healthtracker.common.CsvWriter;
import com.healthtracker.medication.DoseService;
import com.healthtracker.medication.MedicationService;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.symptom.SymptomService;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds CSV files that can be handed to a doctor or opened in a spreadsheet. */
@Service
public class ExportService {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VitalService vitalService;
    private final MedicationService medicationService;
    private final DoseService doseService;
    private final SymptomService symptomService;
    private final ActivityService activityService;
    private final UserService userService;
    private final Clock clock;

    public ExportService(
            VitalService vitalService,
            MedicationService medicationService,
            DoseService doseService,
            SymptomService symptomService,
            ActivityService activityService,
            UserService userService,
            Clock clock) {
        this.vitalService = vitalService;
        this.medicationService = medicationService;
        this.doseService = doseService;
        this.symptomService = symptomService;
        this.activityService = activityService;
        this.userService = userService;
        this.clock = clock;
    }

    public record Export(String filename, String content) {}

    @Transactional(readOnly = true)
    public Export export(Long userId, ExportDataset dataset, LocalDate from, LocalDate to) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(zone));
        LocalDate start = from != null ? from : end.minusDays(89);
        String content = switch (dataset) {
            case VITALS -> vitals(userId, start, end, zone);
            case MEDICATIONS -> medications(userId);
            case DOSES -> doses(userId, start, end, zone);
            case SYMPTOMS -> symptoms(userId, start, end, zone);
            case ACTIVITIES -> activities(userId, start, end, zone);
        };
        String filename = dataset == ExportDataset.MEDICATIONS
                ? "health-tracker-medications.csv"
                : "health-tracker-%s-%s_%s.csv".formatted(dataset.fileStem(), start, end);
        return new Export(filename, content);
    }

    private String vitals(Long userId, LocalDate from, LocalDate to, ZoneId zone) {
        CsvWriter csv = new CsvWriter()
                .row("measured_at", "type", "value", "secondary_value", "unit", "glucose_context", "flag", "notes");
        vitalService.readings(userId, null, from, to, false).reversed().forEach(r -> csv.row(
                local(r.measuredAt(), zone),
                r.type(),
                r.value(),
                r.secondaryValue(),
                r.unit(),
                r.glucoseContext(),
                r.flag(),
                r.notes()));
        return csv.toString();
    }

    private String medications(Long userId) {
        CsvWriter csv = new CsvWriter()
                .row("name", "dosage", "schedule", "start_date", "end_date", "active", "instructions");
        for (MedicationResponse m : medicationService.list(userId, true)) {
            String schedule = m.asNeeded()
                    ? "as needed"
                    : m.times().stream().map(String::valueOf).collect(Collectors.joining(" "));
            csv.row(m.name(), m.dosage(), schedule, m.startDate(), m.endDate(), m.active(), m.instructions());
        }
        return csv.toString();
    }

    private String doses(Long userId, LocalDate from, LocalDate to, ZoneId zone) {
        CsvWriter csv = new CsvWriter()
                .row("scheduled_at", "medication", "dosage", "status", "recorded_at", "as_needed", "note");
        for (DoseResponse d : doseService.history(userId, from, to, false)) {
            csv.row(
                    local(d.scheduledAt(), zone),
                    d.medicationName(),
                    d.dosage(),
                    d.status(),
                    local(d.recordedAt(), zone),
                    d.asNeeded(),
                    d.note());
        }
        return csv.toString();
    }

    private String symptoms(Long userId, LocalDate from, LocalDate to, ZoneId zone) {
        CsvWriter csv = new CsvWriter().row("occurred_at", "symptom", "severity", "duration_minutes", "notes");
        symptomService.entries(userId, from, to, null).reversed().forEach(s -> csv.row(
                local(s.occurredAt(), zone), s.name(), s.severity(), s.durationMinutes(), s.notes()));
        return csv.toString();
    }

    private String activities(Long userId, LocalDate from, LocalDate to, ZoneId zone) {
        CsvWriter csv = new CsvWriter()
                .row("started_at", "type", "duration_minutes", "distance_km", "steps", "calories_kcal", "notes");
        activityService.entries(userId, from, to, null).reversed().forEach(a -> csv.row(
                local(a.startedAt(), zone),
                a.type(),
                a.durationMinutes(),
                a.distanceKm(),
                a.steps(),
                a.caloriesKcal(),
                a.notes()));
        return csv.toString();
    }

    private static String local(Instant instant, ZoneId zone) {
        return instant == null ? null : LOCAL.format(instant.atZone(zone));
    }
}
