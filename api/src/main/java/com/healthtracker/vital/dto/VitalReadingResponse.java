package com.healthtracker.vital.dto;

import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.time.Instant;

public record VitalReadingResponse(
        Long id,
        VitalType type,
        BigDecimal value,
        BigDecimal secondaryValue,
        String unit,
        GlucoseContext glucoseContext,
        Instant measuredAt,
        String notes) {

    public static VitalReadingResponse from(VitalReading r) {
        return new VitalReadingResponse(
                r.getId(),
                r.getType(),
                r.getValue(),
                r.getSecondaryValue(),
                r.getType().unit(),
                r.getGlucoseContext(),
                r.getMeasuredAt(),
                r.getNotes());
    }
}
