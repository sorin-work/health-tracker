package com.healthtracker.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.healthtracker.activity.ActivityService;
import com.healthtracker.medication.DoseService;
import com.healthtracker.medication.DoseStatus;
import com.healthtracker.medication.MedicationService;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.range.Flag;
import com.healthtracker.symptom.SymptomService;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalService;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingResponse;
import java.math.BigDecimal;
import java.time.Clock;
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
    @Mock UserService userService;

    ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService(vitalService, medicationService, doseService, symptomService, activityService,
                userService, Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), BERLIN));
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
                new MedicationResponse(2L, "Ibuprofen", "200 mg", "max 3 a day", true, List.of(), FROM, null, true)));

        ExportService.Export export = service.export(1L, ExportDataset.MEDICATIONS, null, null);

        assertThat(export.filename()).isEqualTo("health-tracker-medications.csv");
        assertThat(export.content())
                .contains("Metformin,500 mg,08:00 20:00,2026-02-01,,true,")
                .contains("Ibuprofen,200 mg,as needed,2026-02-01,,true,max 3 a day");
    }

    @Test
    void defaultsToTheLast90Days() {
        when(symptomService.entries(1L, LocalDate.of(2025, 12, 2), LocalDate.of(2026, 3, 1), null))
                .thenReturn(List.of(new SymptomResponse(
                        1L, "Headache", 4, Instant.parse("2026-02-01T10:00:00Z"), 60, null)));

        ExportService.Export export = service.export(1L, ExportDataset.SYMPTOMS, null, null);

        assertThat(export.filename()).isEqualTo("health-tracker-symptoms-2025-12-02_2026-03-01.csv");
        assertThat(export.content()).contains("2026-02-01 11:00,Headache,4,60,");
    }
}
