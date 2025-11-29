package com.healthtracker.vital.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of readings of a single type, aggregated for charts. */
public record TrendPoint(
        LocalDate date,
        BigDecimal average,
        BigDecimal min,
        BigDecimal max,
        BigDecimal averageSecondary,
        int count,
        int flagged) {}
