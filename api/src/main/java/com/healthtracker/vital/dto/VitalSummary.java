package com.healthtracker.vital.dto;

import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;

/** Latest reading of a type plus a 30 day average, for the dashboard cards. */
public record VitalSummary(
        VitalType type,
        String unit,
        VitalReadingResponse latest,
        BigDecimal average30d,
        BigDecimal averageSecondary30d,
        int count30d,
        int flagged30d) {}
