package com.healthtracker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.healthtracker.activity.ActivityService;
import com.healthtracker.activity.ActivityType;
import com.healthtracker.activity.dto.ActivityResponse;
import com.healthtracker.journal.JournalService;
import com.healthtracker.medication.AdherenceStats;
import com.healthtracker.medication.DoseService;
import com.healthtracker.medication.DoseStatus;
import com.healthtracker.medication.MedicationService;
import com.healthtracker.medication.dto.AdherenceResponse;
import com.healthtracker.medication.dto.AdherenceResponse.MedicationAdherence;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.mood.MoodService;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.range.Flag;
import com.healthtracker.symptom.SymptomService;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.User;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalService;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 28);
    private static final AdherenceStats GOOD = new AdherenceStats(28, 26, 1, 1, 0, 92.9);

    @Mock UserService userService;
    @Mock VitalService vitalService;
    @Mock MedicationService medicationService;
    @Mock DoseService doseService;
    @Mock SymptomService symptomService;
    @Mock MoodService moodService;
    @Mock ActivityService activityService;
    @Mock JournalService journalService;

    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(userService, vitalService, medicationService, doseService, symptomService,
                moodService, activityService, journalService, Clock.fixed(NOW, ZoneOffset.UTC));
        User user = new User("alex@example.com", "hash", "Alex Demo", "UTC", Instant.EPOCH);
        lenient().when(userService.get(1L)).thenReturn(user);
        lenient().when(doseService.adherence(eq(1L), any(), any()))
                .thenReturn(new AdherenceResponse(FROM, TO, GOOD, List.of(), List.of()));
    }

    private static VitalReadingResponse bp(String sys, String dia, Flag flag) {
        return new VitalReadingResponse(1L, VitalType.BLOOD_PRESSURE, new BigDecimal(sys), new BigDecimal(dia), "mmHg",
                null, Instant.parse("2026-09-10T07:00:00Z"), null, flag);
    }

    @Test
    void summarisesEachVitalAndListsFlaggedReadings() {
        when(vitalService.readings(1L, null, FROM, TO, false)).thenReturn(List.of(
                bp("150", "95", Flag.HIGH), bp("120", "80", Flag.NORMAL), bp("130", "84", Flag.NORMAL)));

        DoctorReport report = service.report(1L, FROM, TO);

        assertThat(report.patient()).isEqualTo("Alex Demo");
        assertThat(report.vitals()).singleElement().satisfies(v -> {
            assertThat(v.type()).isEqualTo(VitalType.BLOOD_PRESSURE);
            assertThat(v.count()).isEqualTo(3);
            assertThat(v.average()).isEqualByComparingTo("133.3");
            assertThat(v.averageSecondary()).isEqualByComparingTo("86.3");
            assertThat(v.min()).isEqualByComparingTo("120");
            assertThat(v.max()).isEqualByComparingTo("150");
            assertThat(v.flagged()).isEqualTo(1);
        });
        assertThat(report.flaggedReadings()).hasSize(1);
        assertThat(report.flaggedTotal()).isEqualTo(1);
        assertThat(report.adherence().percent()).isEqualTo(92.9);
    }

    @Test
    void includesMedicationsTakenDuringThePeriodOnly() {
        MedicationResponse metformin = new MedicationResponse(1L, "Metformin", "500 mg", null, false,
                List.of(LocalTime.of(8, 0)), LocalDate.of(2026, 1, 1), null, true);
        MedicationResponse stoppedBefore = new MedicationResponse(2L, "Amoxicillin", "500 mg", null, false,
                List.of(LocalTime.of(8, 0)), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7), false);
        MedicationResponse ibuprofen = new MedicationResponse(3L, "Ibuprofen", "200 mg", null, true,
                List.of(), LocalDate.of(2026, 1, 1), null, true);
        when(medicationService.list(1L, true)).thenReturn(List.of(ibuprofen, metformin, stoppedBefore));
        when(doseService.adherence(1L, FROM, TO)).thenReturn(new AdherenceResponse(FROM, TO, GOOD,
                List.of(new MedicationAdherence(1L, "Metformin", GOOD)), List.of()));
        Instant at = Instant.parse("2026-09-12T14:00:00Z");
        when(doseService.history(eq(1L), eq(FROM), eq(TO), anyBoolean())).thenReturn(List.of(
                new DoseResponse(3L, "Ibuprofen", "200 mg", true, at, DoseStatus.TAKEN, 5L, at, null),
                new DoseResponse(3L, "Ibuprofen", "200 mg", true, at.plusSeconds(86_400), DoseStatus.TAKEN, 6L, at, null)));

        DoctorReport report = service.report(1L, FROM, TO);

        assertThat(report.medications()).extracting(DoctorReport.MedicationLine::name)
                .containsExactly("Metformin", "Ibuprofen");
        assertThat(report.medications().get(0).adherence()).isEqualTo(GOOD);
        assertThat(report.medications().get(0).schedule()).isEqualTo("DAILY");
        assertThat(report.medications().get(1).schedule()).isEqualTo("AS_NEEDED");
        assertThat(report.medications().get(1).asNeededTaken()).isEqualTo(2);
    }

    @Test
    void groupsSymptomsAndAveragesMoodAndActivity() {
        Instant at = Instant.parse("2026-09-05T10:00:00Z");
        when(symptomService.entries(1L, FROM, TO, null)).thenReturn(List.of(
                new SymptomResponse(1L, "Headache", 4, at, null, null),
                new SymptomResponse(2L, "Headache", 7, at, null, null),
                new SymptomResponse(3L, "Nausea", 2, at, null, null)));
        when(moodService.entries(1L, FROM, TO)).thenReturn(List.of(
                new MoodResponse(1L, 4, 3, at, null), new MoodResponse(2L, 3, null, at, null)));
        when(activityService.entries(1L, FROM, TO, null)).thenReturn(List.of(
                new ActivityResponse(1L, ActivityType.WALK, at, 300, null, null, null, null),
                new ActivityResponse(2L, ActivityType.RUN, at, 300, null, null, null, null)));

        DoctorReport report = service.report(1L, FROM, TO);

        assertThat(report.symptoms()).extracting(DoctorReport.SymptomStats::name).containsExactly("Headache", "Nausea");
        assertThat(report.symptoms().getFirst().averageSeverity()).isEqualByComparingTo("5.5");
        assertThat(report.symptoms().getFirst().maxSeverity()).isEqualTo(7);
        assertThat(report.mood().averageMood()).isEqualByComparingTo("3.5");
        assertThat(report.mood().averageEnergy()).isEqualByComparingTo("3.0");
        // 600 minutes over four weeks.
        assertThat(report.activity().averageMinutesPerWeek()).isEqualByComparingTo("150");
    }

    @Test
    void defaultsToTheLast90DaysAndLimitsThePeriod() {
        DoctorReport report = service.report(1L, null, null);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(report.to()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThatThrownBy(() -> service.report(1L, LocalDate.of(2025, 1, 1), TO)).hasMessageContaining("at most");
        assertThatThrownBy(() -> service.report(1L, TO, FROM)).hasMessageContaining("before");
    }
}
