package com.healthtracker.range;

import java.math.BigDecimal;

/**
 * Bounds for one metric. Values in [low, high] are normal. Values at or beyond a critical bound are
 * flagged critical. Any bound may be null when it does not apply (e.g. no critical high for sleep).
 */
public record ReferenceRange(BigDecimal criticalLow, BigDecimal low, BigDecimal high, BigDecimal criticalHigh) {

    public static ReferenceRange of(Number criticalLow, Number low, Number high, Number criticalHigh) {
        return new ReferenceRange(dec(criticalLow), dec(low), dec(high), dec(criticalHigh));
    }

    public ReferenceRange withBounds(BigDecimal newLow, BigDecimal newHigh) {
        return new ReferenceRange(criticalLow, newLow, newHigh, criticalHigh);
    }

    public Flag classify(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (criticalHigh != null && value.compareTo(criticalHigh) >= 0) {
            return Flag.CRITICAL_HIGH;
        }
        if (criticalLow != null && value.compareTo(criticalLow) <= 0) {
            return Flag.CRITICAL_LOW;
        }
        if (high != null && value.compareTo(high) > 0) {
            return Flag.HIGH;
        }
        if (low != null && value.compareTo(low) < 0) {
            return Flag.LOW;
        }
        return Flag.NORMAL;
    }

    private static BigDecimal dec(Number n) {
        return n == null ? null : new BigDecimal(n.toString());
    }
}
