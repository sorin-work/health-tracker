package com.healthtracker.medication;

import static com.healthtracker.medication.MedicationFixtures.asNeeded;
import static com.healthtracker.medication.MedicationFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class SupplyEstimatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    private static Medication withSupply(Medication m, String supply) {
        m.setSupply(new BigDecimal(supply));
        return m;
    }

    @Test
    void twiceADayHalvesTheDays() {
        Medication m = withSupply(scheduled(1, "Metformin", TODAY.minusDays(30), null, "08:00", "20:00"), "30");

        assertThat(SupplyEstimator.daysLeft(m)).isEqualTo(15);
    }

    @Test
    void accountsForTheDoseQuantity() {
        Medication m = withSupply(scheduled(1, "Paracetamol", TODAY, null, "08:00"), "30");
        m.setDoseQuantity(new BigDecimal("2"));

        assertThat(SupplyEstimator.daysLeft(m)).isEqualTo(15);
    }

    @Test
    void everyOtherDayAndWeeklyLastLonger() {
        Medication everyOther = withSupply(scheduled(1, "Prednisolone", TODAY, null, "08:00"), "10");
        everyOther.setScheduleType(ScheduleType.EVERY_N_DAYS);
        everyOther.setIntervalDays(2);
        Medication weekly = withSupply(scheduled(2, "Vitamin D3", TODAY, null, "09:00"), "4");
        weekly.setScheduleType(ScheduleType.WEEKLY);
        weekly.setDaysOfWeek(EnumSet.of(DayOfWeek.SUNDAY));

        assertThat(SupplyEstimator.daysLeft(everyOther)).isEqualTo(20);
        assertThat(SupplyEstimator.daysLeft(weekly)).isEqualTo(28);
    }

    @Test
    void untrackedSupplyHasNoEstimate() {
        assertThat(SupplyEstimator.daysLeft(scheduled(1, "Metformin", TODAY, null, "08:00"))).isNull();
        assertThat(SupplyEstimator.isLow(scheduled(1, "Metformin", TODAY, null, "08:00"), TODAY)).isFalse();
    }

    @Test
    void lowWithinTheWarningWindow() {
        Medication m = withSupply(scheduled(1, "Lisinopril", TODAY.minusDays(30), null, "08:00"), "7");

        assertThat(SupplyEstimator.isLow(m, TODAY)).isTrue();
        m.setSupply(new BigDecimal("8"));
        assertThat(SupplyEstimator.isLow(m, TODAY)).isFalse();
        m.setRefillWarningDays(10);
        assertThat(SupplyEstimator.isLow(m, TODAY)).isTrue();
    }

    @Test
    void stoppedOrFinishedMedicationsAreNeverLow() {
        Medication stopped = withSupply(scheduled(1, "Lisinopril", TODAY.minusDays(30), null, "08:00"), "0");
        stopped.setActive(false);
        Medication finished = withSupply(scheduled(2, "Amoxicillin", TODAY.minusDays(9), TODAY.minusDays(2), "08:00"), "0");

        assertThat(SupplyEstimator.isLow(stopped, TODAY)).isFalse();
        assertThat(SupplyEstimator.isLow(finished, TODAY)).isFalse();
    }

    @Test
    void asNeededIsLowWhenNotEvenOneDoseIsLeft() {
        Medication ibuprofen = withSupply(asNeeded(1, "Ibuprofen", TODAY.minusDays(30)), "0.5");

        assertThat(SupplyEstimator.daysLeft(ibuprofen)).isNull();
        assertThat(SupplyEstimator.isLow(ibuprofen, TODAY)).isTrue();
    }
}
