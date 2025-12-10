package com.healthtracker.medication;

import static com.healthtracker.medication.MedicationFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdherenceCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 10);
    private final Medication a = scheduled(1, "A", DAY, null, "08:00");
    private final Medication b = scheduled(2, "B", DAY, null, "08:00");

    private static ScheduledDose dose(Medication m, LocalDate day, DoseStatus status) {
        return new ScheduledDose(m, day, day.atStartOfDay().toInstant(ZoneOffset.UTC), null, status);
    }

    @Test
    void percentIsTakenOverDueDoses() {
        List<ScheduledDose> doses = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            doses.add(dose(a, DAY, DoseStatus.TAKEN));
        }
        doses.add(dose(a, DAY, DoseStatus.SKIPPED));
        doses.add(dose(a, DAY, DoseStatus.MISSED));

        AdherenceStats stats = AdherenceCalculator.summarize(doses);

        assertThat(stats.scheduled()).isEqualTo(10);
        assertThat(stats.taken()).isEqualTo(8);
        assertThat(stats.skipped()).isEqualTo(1);
        assertThat(stats.missed()).isEqualTo(1);
        assertThat(stats.pending()).isZero();
        assertThat(stats.percent()).isEqualTo(80.0);
    }

    @Test
    void percentIsNullWhenNothingWasScheduled() {
        assertThat(AdherenceCalculator.summarize(List.of()).percent()).isNull();
    }

    @Test
    void roundsToOneDecimal() {
        AdherenceStats stats = AdherenceCalculator.summarize(List.of(
                dose(a, DAY, DoseStatus.TAKEN), dose(a, DAY, DoseStatus.TAKEN), dose(a, DAY, DoseStatus.MISSED)));

        assertThat(stats.percent()).isEqualTo(66.7);
    }

    @Test
    void groupsByMedicationAndDay() {
        List<ScheduledDose> doses = List.of(
                dose(a, DAY, DoseStatus.TAKEN),
                dose(b, DAY, DoseStatus.MISSED),
                dose(a, DAY.plusDays(1), DoseStatus.TAKEN),
                dose(b, DAY.plusDays(1), DoseStatus.TAKEN));

        Map<Long, AdherenceStats> byMedication = AdherenceCalculator.byMedication(doses);
        Map<LocalDate, AdherenceStats> byDay = AdherenceCalculator.byDay(doses);

        assertThat(byMedication.get(1L).percent()).isEqualTo(100.0);
        assertThat(byMedication.get(2L).percent()).isEqualTo(50.0);
        assertThat(byDay.keySet()).containsExactly(DAY, DAY.plusDays(1));
        assertThat(byDay.get(DAY).percent()).isEqualTo(50.0);
        assertThat(byDay.get(DAY.plusDays(1)).percent()).isEqualTo(100.0);
    }
}
