package com.healthtracker.range;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RangeFlaggerTest {

    private final RangeFlagger flagger = RangeFlagger.withDefaults();

    @ParameterizedTest
    @CsvSource({
        "35, CRITICAL_LOW",
        "40, CRITICAL_LOW",
        "45, LOW",
        "50, NORMAL",
        "72, NORMAL",
        "100, NORMAL",
        "101, HIGH",
        "130, CRITICAL_HIGH"
    })
    void flagsHeartRate(String bpm, Flag expected) {
        assertThat(flagger.flag(VitalType.HEART_RATE, new BigDecimal(bpm), null, null)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "118, 76, NORMAL",
        "135, 76, HIGH",
        "118, 85, HIGH",
        "85, 55, LOW",
        "182, 95, CRITICAL_HIGH",
        "150, 121, CRITICAL_HIGH"
    })
    void bloodPressureUsesTheWorseOfBothNumbers(String sys, String dia, Flag expected) {
        assertThat(flagger.flag(VitalType.BLOOD_PRESSURE, new BigDecimal(sys), new BigDecimal(dia), null))
                .isEqualTo(expected);
    }

    @Test
    void highSystolicDoesNotHideCriticalDiastolic() {
        assertThat(flagger.flag(VitalType.BLOOD_PRESSURE, new BigDecimal("150"), new BigDecimal("125"), null))
                .isEqualTo(Flag.CRITICAL_HIGH);
    }

    @Test
    void glucoseRangeDependsOnMealContext() {
        BigDecimal value = new BigDecimal("120");
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, value, null, GlucoseContext.FASTING)).isEqualTo(Flag.HIGH);
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, value, null, GlucoseContext.BEFORE_MEAL)).isEqualTo(Flag.HIGH);
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, value, null, GlucoseContext.AFTER_MEAL)).isEqualTo(Flag.NORMAL);
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, value, null, null)).isEqualTo(Flag.NORMAL);
    }

    @Test
    void lowGlucoseIsCriticalBelow54() {
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, new BigDecimal("60"), null, GlucoseContext.RANDOM))
                .isEqualTo(Flag.LOW);
        assertThat(flagger.flag(VitalType.BLOOD_GLUCOSE, new BigDecimal("50"), null, GlucoseContext.RANDOM))
                .isEqualTo(Flag.CRITICAL_LOW);
    }

    @Test
    void sleepHasNoCriticalHigh() {
        assertThat(flagger.flag(VitalType.SLEEP, new BigDecimal("6.5"), null, null)).isEqualTo(Flag.LOW);
        assertThat(flagger.flag(VitalType.SLEEP, new BigDecimal("8"), null, null)).isEqualTo(Flag.NORMAL);
        assertThat(flagger.flag(VitalType.SLEEP, new BigDecimal("12"), null, null)).isEqualTo(Flag.HIGH);
        assertThat(flagger.flag(VitalType.SLEEP, new BigDecimal("3"), null, null)).isEqualTo(Flag.CRITICAL_LOW);
    }

    @Test
    void weightIsNotAssessed() {
        assertThat(flagger.flag(VitalType.WEIGHT, new BigDecimal("140"), null, null)).isNull();
    }

    @Test
    void customBoundsReplaceDefaultsButKeepCriticalLimits() {
        RangeFlagger custom = new RangeFlagger(Map.of(
                RangeMetric.SYSTOLIC,
                RangeMetric.SYSTOLIC.defaults().withBounds(new BigDecimal("100"), new BigDecimal("140"))));

        assertThat(custom.flag(VitalType.BLOOD_PRESSURE, new BigDecimal("135"), new BigDecimal("75"), null))
                .isEqualTo(Flag.NORMAL);
        assertThat(custom.flag(VitalType.BLOOD_PRESSURE, new BigDecimal("95"), new BigDecimal("75"), null))
                .isEqualTo(Flag.LOW);
        assertThat(custom.flag(VitalType.BLOOD_PRESSURE, new BigDecimal("185"), new BigDecimal("75"), null))
                .isEqualTo(Flag.CRITICAL_HIGH);
    }

    @Test
    void worstPrefersHigherSeverity() {
        assertThat(Flag.worst(Flag.NORMAL, Flag.HIGH)).isEqualTo(Flag.HIGH);
        assertThat(Flag.worst(Flag.CRITICAL_LOW, Flag.HIGH)).isEqualTo(Flag.CRITICAL_LOW);
        assertThat(Flag.worst(null, Flag.LOW)).isEqualTo(Flag.LOW);
        assertThat(Flag.worst(Flag.LOW, Flag.HIGH)).isEqualTo(Flag.LOW);
    }
}
