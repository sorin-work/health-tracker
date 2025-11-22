package com.healthtracker.vital;

import com.healthtracker.common.ApiException;
import com.healthtracker.vital.dto.VitalReadingRequest;
import java.math.BigDecimal;
import java.time.Instant;

/** Catches typos (a weight of 7000 kg, a missing diastolic value) before they skew the charts. */
public final class VitalValidator {

    private VitalValidator() {}

    public static void validate(VitalReadingRequest r, Instant now) {
        switch (r.type()) {
            case WEIGHT -> within(r.value(), 20, 400, "Weight");
            case HEART_RATE -> within(r.value(), 20, 250, "Heart rate");
            case BLOOD_GLUCOSE -> within(r.value(), 10, 700, "Blood glucose");
            case SLEEP -> {
                within(r.value(), 0, 24, "Sleep");
                if (r.value().signum() == 0) {
                    throw ApiException.badRequest("Sleep must be more than 0 hours");
                }
            }
            case BLOOD_PRESSURE -> {
                if (r.secondaryValue() == null) {
                    throw ApiException.badRequest("Blood pressure needs both systolic and diastolic values");
                }
                within(r.value(), 50, 260, "Systolic pressure");
                within(r.secondaryValue(), 30, 160, "Diastolic pressure");
                if (r.value().compareTo(r.secondaryValue()) <= 0) {
                    throw ApiException.badRequest("Systolic pressure must be higher than diastolic");
                }
            }
        }
        if (r.type() != VitalType.BLOOD_PRESSURE && r.secondaryValue() != null) {
            throw ApiException.badRequest("Only blood pressure takes a second value");
        }
        if (r.type() != VitalType.BLOOD_GLUCOSE && r.glucoseContext() != null) {
            throw ApiException.badRequest("Only glucose readings take a meal context");
        }
    }

    private static void within(BigDecimal value, int min, int max, String what) {
        if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw ApiException.badRequest(what + " must be between " + min + " and " + max);
        }
    }
}
