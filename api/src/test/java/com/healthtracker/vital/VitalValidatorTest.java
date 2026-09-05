package com.healthtracker.vital;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.healthtracker.common.ApiException;
import com.healthtracker.vital.dto.VitalReadingRequest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VitalValidatorTest {

    private static final Instant NOW = Instant.parse("2026-02-10T12:00:00Z");

    private static VitalReadingRequest req(VitalType type, String value, String secondary, GlucoseContext ctx) {
        return new VitalReadingRequest(
                type, new BigDecimal(value), secondary == null ? null : new BigDecimal(secondary), ctx,
                NOW.minusSeconds(60), null);
    }

    @Test
    void acceptsPlausibleReadings() {
        assertThatCode(() -> VitalValidator.validate(req(VitalType.WEIGHT, "82.4", null, null), NOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> VitalValidator.validate(req(VitalType.BLOOD_PRESSURE, "128", "82", null), NOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> VitalValidator.validate(
                        req(VitalType.BLOOD_GLUCOSE, "105", null, GlucoseContext.FASTING), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void bloodPressureNeedsDiastolic() {
        assertThatThrownBy(() -> VitalValidator.validate(req(VitalType.BLOOD_PRESSURE, "128", null, null), NOW))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("diastolic");
    }

    @Test
    void systolicMustExceedDiastolic() {
        assertThatThrownBy(() -> VitalValidator.validate(req(VitalType.BLOOD_PRESSURE, "80", "120", null), NOW))
                .hasMessageContaining("higher than diastolic");
    }

    @Test
    void rejectsImplausibleValues() {
        assertThatThrownBy(() -> VitalValidator.validate(req(VitalType.WEIGHT, "824", null, null), NOW))
                .hasMessageContaining("between 20 and 400");
        assertThatThrownBy(() -> VitalValidator.validate(req(VitalType.SLEEP, "0", null, null), NOW))
                .hasMessageContaining("more than 0");
    }

    @Test
    void rejectsFieldsThatDoNotBelongToTheType() {
        assertThatThrownBy(() -> VitalValidator.validate(req(VitalType.HEART_RATE, "70", "60", null), NOW))
                .hasMessageContaining("Only blood pressure");
        assertThatThrownBy(() -> VitalValidator.validate(
                        req(VitalType.WEIGHT, "80", null, GlucoseContext.FASTING), NOW))
                .hasMessageContaining("Only glucose");
    }

    @Test
    void rejectsReadingsInTheFuture() {
        var future = new VitalReadingRequest(
                VitalType.WEIGHT, new BigDecimal("80"), null, null, NOW.plusSeconds(3600), null);
        assertThatThrownBy(() -> VitalValidator.validate(future, NOW)).hasMessageContaining("future");
    }
}
