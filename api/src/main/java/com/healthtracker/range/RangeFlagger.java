package com.healthtracker.range;

import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.util.Map;

/** Pure logic that decides whether a reading is out of range. */
public final class RangeFlagger {

    private final Map<RangeMetric, ReferenceRange> ranges;

    public RangeFlagger(Map<RangeMetric, ReferenceRange> ranges) {
        this.ranges = ranges;
    }

    public static RangeFlagger withDefaults() {
        return new RangeFlagger(Map.of());
    }

    /**
     * @return the flag, or {@code null} for metrics we do not assess (weight)
     */
    public Flag flag(VitalType type, BigDecimal value, BigDecimal secondary, GlucoseContext context) {
        return switch (type) {
            case WEIGHT -> null;
            case HEART_RATE -> range(RangeMetric.HEART_RATE).classify(value);
            case SLEEP -> range(RangeMetric.SLEEP).classify(value);
            case BLOOD_GLUCOSE -> range(glucoseMetric(context)).classify(value);
            case BLOOD_PRESSURE -> Flag.worst(
                    range(RangeMetric.SYSTOLIC).classify(value), range(RangeMetric.DIASTOLIC).classify(secondary));
        };
    }

    public ReferenceRange range(RangeMetric metric) {
        return ranges.getOrDefault(metric, metric.defaults());
    }

    public static RangeMetric glucoseMetric(GlucoseContext context) {
        return context != null && context.fasting() ? RangeMetric.GLUCOSE_FASTING : RangeMetric.GLUCOSE_NON_FASTING;
    }
}
