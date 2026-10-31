package com.healthtracker.vital.dto;

import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;

/**
 * Average of the last 7 days next to the 7 days before. {@code change} is this week minus last week,
 * null when either week has no readings. For blood pressure the secondary values are diastolic.
 */
public record WeeklyChange(
        VitalType type,
        int count,
        BigDecimal average,
        BigDecimal averageSecondary,
        int previousCount,
        BigDecimal previousAverage,
        BigDecimal previousAverageSecondary,
        BigDecimal change,
        BigDecimal changeSecondary) {}
