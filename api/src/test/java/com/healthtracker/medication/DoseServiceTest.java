package com.healthtracker.medication;

import static com.healthtracker.medication.MedicationFixtures.asNeeded;
import static com.healthtracker.medication.MedicationFixtures.log;
import static com.healthtracker.medication.MedicationFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.medication.dto.AdherenceResponse;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.RecordDoseRequest;
import com.healthtracker.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoseServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    /** 2026-01-20 13:00 in Berlin. */
    private static final Instant NOW = Instant.parse("2026-01-20T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 20);

    @Mock
    MedicationRepository medications;

    @Mock
    DoseLogRepository doseLogs;

    @Mock
    UserService userService;

    DoseService service;

    @BeforeEach
    void setUp() {
        service = new DoseService(medications, doseLogs, userService, Clock.fixed(NOW, BERLIN));
        lenient().when(userService.zoneOf(1L)).thenReturn(BERLIN);
    }

    @Test
    void todaysScheduleShowsTakenAndPendingDoses() {
        Medication metformin = scheduled(1, "Metformin", TODAY.minusDays(30), null, "08:00", "20:00");
        Instant morning = Instant.parse("2026-01-20T07:00:00Z");
        when(medications.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of(metformin));
        when(doseLogs.findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(eq(1L), any(), any()))
                .thenReturn(List.of(log(7, metformin, morning, DoseStatus.TAKEN)));

        List<DoseResponse> doses = service.forDay(1L, null);

        assertThat(doses).extracting(DoseResponse::status).containsExactly(DoseStatus.TAKEN, DoseStatus.PENDING);
        assertThat(doses.get(0).logId()).isEqualTo(7L);
        assertThat(doses.get(1).scheduledAt()).isEqualTo(Instant.parse("2026-01-20T19:00:00Z"));
    }

    @Test
    void scheduleIncludesAsNeededDosesThatWereTaken() {
        Medication ibuprofen = asNeeded(2, "Ibuprofen", TODAY.minusDays(30));
        Instant taken = Instant.parse("2026-01-20T10:15:00Z");
        when(medications.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of(ibuprofen));
        when(doseLogs.findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(eq(1L), any(), any()))
                .thenReturn(List.of(log(3, ibuprofen, taken, DoseStatus.TAKEN)));

        List<DoseResponse> doses = service.forDay(1L, TODAY);

        assertThat(doses).singleElement().satisfies(d -> {
            assertThat(d.asNeeded()).isTrue();
            assertThat(d.scheduledAt()).isEqualTo(taken);
        });
    }

    @Test
    void recordsADoseForAScheduledSlot() {
        Medication m = scheduled(1, "Lisinopril", TODAY.minusDays(5), null, "08:00");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        when(medications.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.empty());
        when(doseLogs.save(any(DoseLog.class))).thenAnswer(inv -> inv.getArgument(0));

        DoseResponse response = service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.TAKEN, " "));

        assertThat(response.status()).isEqualTo(DoseStatus.TAKEN);
        assertThat(response.recordedAt()).isEqualTo(NOW);
        assertThat(response.note()).isNull();
    }

    @Test
    void recordingTwiceUpdatesTheExistingLog() {
        Medication m = scheduled(1, "Lisinopril", TODAY.minusDays(5), null, "08:00");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        DoseLog existing = log(9, m, slot, DoseStatus.SKIPPED);
        when(medications.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.of(existing));
        when(doseLogs.save(existing)).thenReturn(existing);

        DoseResponse response = service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.TAKEN, null));

        assertThat(response.logId()).isEqualTo(9L);
        assertThat(existing.getStatus()).isEqualTo(DoseStatus.TAKEN);
    }

    @Test
    void rejectsATimeThatIsNotOnTheSchedule() {
        Medication m = scheduled(1, "Lisinopril", TODAY.minusDays(5), null, "08:00");
        when(medications.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.record(
                        1L, 1L, new RecordDoseRequest(Instant.parse("2026-01-20T08:30:00Z"), DoseStatus.TAKEN, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No dose of Lisinopril");
        verify(doseLogs, never()).save(any());
    }

    @Test
    void asNeededDosesCanOnlyBeTaken() {
        Medication m = asNeeded(2, "Ibuprofen", TODAY.minusDays(5));
        when(medications.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.record(1L, 2L, new RecordDoseRequest(NOW, DoseStatus.SKIPPED, null)))
                .hasMessageContaining("only be logged as taken");
    }

    @Test
    void missedAndPendingAreNotValidInput() {
        Medication m = scheduled(1, "Lisinopril", TODAY.minusDays(5), null, "08:00");
        when(medications.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.record(
                        1L, 1L, new RecordDoseRequest(Instant.parse("2026-01-20T07:00:00Z"), DoseStatus.MISSED, null)))
                .hasMessageContaining("taken or skipped");
    }

    @Test
    void adherenceCountsMissedDosesButNotTodaysPendingOnes() {
        // Started three days ago, one dose a day at 08:00 and one at 20:00.
        Medication m = scheduled(1, "Metformin", TODAY.minusDays(2), null, "08:00", "20:00");
        when(medications.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of(m));
        when(doseLogs.findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(eq(1L), any(), any()))
                .thenReturn(List.of(
                        log(1, m, Instant.parse("2026-01-18T07:00:00Z"), DoseStatus.TAKEN),
                        log(2, m, Instant.parse("2026-01-18T19:00:00Z"), DoseStatus.TAKEN),
                        log(3, m, Instant.parse("2026-01-19T07:00:00Z"), DoseStatus.TAKEN),
                        // 19th evening: nothing logged -> missed
                        log(4, m, Instant.parse("2026-01-20T07:00:00Z"), DoseStatus.TAKEN)));
        // 20th evening is still to come -> pending

        AdherenceResponse adherence = service.adherence(1L, TODAY.minusDays(6), TODAY);

        assertThat(adherence.overall().taken()).isEqualTo(4);
        assertThat(adherence.overall().missed()).isEqualTo(1);
        assertThat(adherence.overall().pending()).isEqualTo(1);
        assertThat(adherence.overall().percent()).isEqualTo(80.0);
        assertThat(adherence.medications()).singleElement().satisfies(ma -> assertThat(ma.name()).isEqualTo("Metformin"));
        assertThat(adherence.daily()).hasSize(3);
    }

    @Test
    void adherenceDefaultsToTheLast30Days() {
        when(medications.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of());

        AdherenceResponse adherence = service.adherence(1L, null, null);

        assertThat(adherence.from()).isEqualTo(TODAY.minusDays(29));
        assertThat(adherence.to()).isEqualTo(TODAY);
        assertThat(adherence.overall().percent()).isNull();
    }

    @Test
    void rejectsRangesLongerThanAYear() {
        assertThatThrownBy(() -> service.adherence(1L, TODAY.minusDays(400), TODAY))
                .hasMessageContaining("at most");
    }

    private Medication trackedLisinopril(String supply) {
        Medication m = scheduled(1, "Lisinopril", TODAY.minusDays(5), null, "08:00");
        m.setSupply(new BigDecimal(supply));
        m.setDoseQuantity(new BigDecimal("2"));
        when(medications.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    void takingADoseUsesUpTheSupply() {
        Medication m = trackedLisinopril("30");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.empty());
        when(doseLogs.save(any(DoseLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.TAKEN, null));

        assertThat(m.getSupply()).isEqualByComparingTo("28");
    }

    @Test
    void takingTheSameDoseTwiceCountsOnce() {
        Medication m = trackedLisinopril("30");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        DoseLog existing = log(9, m, slot, DoseStatus.TAKEN);
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.of(existing));
        when(doseLogs.save(existing)).thenReturn(existing);

        service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.TAKEN, "again"));

        assertThat(m.getSupply()).isEqualByComparingTo("30");
    }

    @Test
    void switchingATakenDoseToSkippedGivesTheSupplyBack() {
        Medication m = trackedLisinopril("30");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        DoseLog existing = log(9, m, slot, DoseStatus.TAKEN);
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.of(existing));
        when(doseLogs.save(existing)).thenReturn(existing);

        service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.SKIPPED, null));

        assertThat(m.getSupply()).isEqualByComparingTo("32");
    }

    @Test
    void undoingATakenDoseGivesTheSupplyBack() {
        Medication m = trackedLisinopril("1");
        DoseLog taken = log(9, m, Instant.parse("2026-01-20T07:00:00Z"), DoseStatus.TAKEN);
        when(doseLogs.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(taken));

        service.undo(1L, 9L);

        assertThat(m.getSupply()).isEqualByComparingTo("3");
        verify(doseLogs).delete(taken);
    }

    @Test
    void supplyNeverGoesNegative() {
        Medication m = trackedLisinopril("1");
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        when(doseLogs.findByMedicationIdAndScheduledAt(1L, slot)).thenReturn(Optional.empty());
        when(doseLogs.save(any(DoseLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(1L, 1L, new RecordDoseRequest(slot, DoseStatus.TAKEN, null));

        assertThat(m.getSupply()).isEqualByComparingTo("0");
    }
}
