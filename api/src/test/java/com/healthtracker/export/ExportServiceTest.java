package com.healthtracker.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.healthtracker.activity.ActivityService;
import com.healthtracker.journal.JournalService;
import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.medication.DoseService;
import com.healthtracker.medication.DoseStatus;
import com.healthtracker.medication.MedicationService;
import com.healthtracker.medication.ScheduleType;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.mood.MoodService;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.range.Flag;
import com.healthtracker.symptom.SymptomService;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalService;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingResponse;
import com.healthtracker.water.WaterService;
import com.healthtracker.water.dto.WaterEntry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final LocalDate FROM = LocalDate.of(2026, 2, 1);
    private static final LocalDate TO = LocalDate.of(2026, 2, 28);

    @Mock VitalService vitalService;
    @Mock MedicationService medicationService;
    @Mock DoseService doseService;
    @Mock SymptomService symptomService;
    @Mock ActivityService activityService;
    @Mock MoodService moodService;
    @Mock WaterService waterService;
    @Mock JournalService journalService;
    @Mock UserService userService;

    ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService(vitalService, medicationService, doseService, symptomService, activityService,
                moodService, waterService, journalService, userService, Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), BERLIN));
        when(userService.zoneOf(1L)).thenReturn(BERLIN);
    }

    @Test
    void vitalsAreOldestFirstInLocalTime() {
        when(vitalService.readings(1L, null, FROM, TO, false)).thenReturn(List.of(
                new VitalReadingResponse(2L, VitalType.BLOOD_PRESSURE, new BigDecimal("142"), new BigDecimal("91"),
                        "mmHg", null, Instant.parse("2026-02-02T19:00:00Z"), "after a run, tired", Flag.HIGH),
                new VitalReadingResponse(1L, VitalType.WEIGHT, new BigDecimal("82.40"), null,
                        "kg", null, Instant.parse("2026-02-01T06:30:00Z"), null, null)));

        ExportService.Export export = service.export(1L, ExportDataset.VITALS, FROM, TO);

        assertThat(export.filename()).isEqualTo("health-tracker-vitals-2026-02-01_2026-02-28.csv");
        assertThat(export.content().split("\r\n")).containsExactly(
                "measured_at,type,value,secondary_value,unit,glucose_context,flag,notes",
                "2026-02-01 07:30,WEIGHT,82.40,,kg,,,",
                "2026-02-02 20:00,BLOOD_PRESSURE,142,91,mmHg,,HIGH,\"after a run, tired\"");
    }

    @Test
    void dosesIncludeMissedOnesButNotPending() {
        Instant slot = Instant.parse("2026-02-10T07:00:00Z");
        when(doseService.history(1L, FROM, TO, false)).thenReturn(List.of(
                new DoseResponse(1L, "Metformin", "500 mg", false, slot, DoseStatus.MISSED, null, null, null)));

        String csv = service.export(1L, ExportDataset.DOSES, FROM, TO).content();

        assertThat(csv).contains("2026-02-10 08:00,Metformin,500 mg,MISSED,,false,");
    }

    @Test
    void medicationsListTheSchedule() {
        when(medicationService.list(1L, true)).thenReturn(List.of(
                new MedicationResponse(1L, "Metformin", "500 mg", null, false,
                        List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), FROM, null, true),
                new MedicationResponse(2L, "Ibuprofen", "200 mg", "max 3 a day", true, List.of(), FROM, null, true),
                new MedicationResponse(3L, "Vitamin D3", null, null, false, List.of(LocalTime.of(9, 0)), FROM, null,
                        true, ScheduleType.WEEKLY, null, List.of(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)),
                new MedicationResponse(4L, "Prednisolone", "5 mg", null, false, List.of(LocalTime.of(8, 0)), FROM, null,
                        true, ScheduleType.EVERY_N_DAYS, 2, List.of())));

        ExportService.Export export = service.export(1L, ExportDataset.MEDICATIONS, null, null);

        assertThat(export.filename()).isEqualTo("health-tracker-medications.csv");
        assertThat(export.content())
                .contains("Metformin,500 mg,08:00 20:00,2026-02-01,,true,")
                .contains("Ibuprofen,200 mg,as needed,2026-02-01,,true,max 3 a day")
                .contains("Vitamin D3,,Wed Sun at 09:00,2026-02-01,,true,")
                .contains("Prednisolone,5 mg,every 2 days at 08:00,2026-02-01,,true,");
    }

    @Test
    void defaultsToTheLast90Days() {
        when(symptomService.entries(1L, LocalDate.of(2025, 12, 2), LocalDate.of(2026, 3, 1), null))
                .thenReturn(List.of(new SymptomResponse(
                        1L, "Headache", 4, Instant.parse("2026-02-01T10:00:00Z"), 60, null)));

        ExportService.Export export = service.export(1L, ExportDataset.SYMPTOMS, null, null);

        assertThat(export.filename()).isEqualTo("health-tracker-symptoms-2025-12-02_2026-03-01.csv");
        assertThat(export.content()).contains("2026-02-01 11:00,Headache,4,60,,");
    }

    @Test
    void symptomTagsShareOneColumn() {
        when(symptomService.entries(1L, FROM, TO, null)).thenReturn(List.of(new SymptomResponse(
                1L, "Migraine", 7, Instant.parse("2026-02-01T10:00:00Z"), null, "Dark room helped",
                List.of("screen time", "stress"))));

        String csv = service.export(1L, ExportDataset.SYMPTOMS, FROM, TO).content();

        assertThat(csv.split("\r\n")).containsExactly(
                "occurred_at,symptom,severity,duration_minutes,notes,tags",
                "2026-02-01 11:00,Migraine,7,,Dark room helped,screen time; stress");
    }

    @Test
    void moodCheckInsAreOldestFirst() {
        when(moodService.entries(1L, FROM, TO)).thenReturn(List.of(
                new MoodResponse(2L, 2, null, Instant.parse("2026-02-03T20:00:00Z"), "Rough day"),
                new MoodResponse(1L, 4, 3, Instant.parse("2026-02-02T07:00:00Z"), null)));

        ExportService.Export export = service.export(1L, ExportDataset.MOOD, FROM, TO);

        assertThat(export.filename()).isEqualTo("health-tracker-mood-2026-02-01_2026-02-28.csv");
        assertThat(export.content().split("\r\n")).containsExactly(
                "recorded_at,mood,energy,note",
                "2026-02-02 08:00,4,3,",
                "2026-02-03 21:00,2,,Rough day");
    }

    @Test
    void waterEntriesInLocalTime() {
        when(waterService.entries(1L, FROM, TO)).thenReturn(List.of(
                new WaterEntry(1L, 250, Instant.parse("2026-02-02T07:00:00Z"))));

        String csv = service.export(1L, ExportDataset.WATER, FROM, TO).content();

        assertThat(csv.split("\r\n")).containsExactly("logged_at,amount_ml", "2026-02-02 08:00,250");
    }

    @Test
    void journalKeepsLineBreaksInsideQuotes() {
        Instant at = Instant.parse("2026-02-02T20:00:00Z");
        when(journalService.entries(1L, FROM, TO, null)).thenReturn(List.of(
                new JournalResponse(1L, LocalDate.of(2026, 2, 2), "Visit", "Bring the log\nAsk about BP", at, at)));

        String csv = service.export(1L, ExportDataset.JOURNAL, FROM, TO).content();

        assertThat(csv).isEqualTo("date,title,note\r\n2026-02-02,Visit,\"Bring the log\nAsk about BP\"\r\n");
    }
}
