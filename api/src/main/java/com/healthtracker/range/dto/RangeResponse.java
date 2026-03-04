package com.healthtracker.range.dto;

import java.math.BigDecimal;

public record RangeResponse(
        String metric,
        String label,
        String unit,
        BigDecimal low,
        BigDecimal high,
        BigDecimal criticalLow,
        BigDecimal criticalHigh,
        BigDecimal defaultLow,
        BigDecimal defaultHigh,
        boolean custom) {}
