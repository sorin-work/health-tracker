package com.healthtracker.range;

/**
 * Metrics that have a reference range. Blood pressure is split in two and glucose depends on
 * whether the reading was taken fasting.
 *
 * <p>Defaults follow common adult guidance (AHA blood pressure categories, ADA glucose targets,
 * 7-9 hours of sleep). They are a starting point; users can set the bounds their doctor gave them.
 */
public enum RangeMetric {
    HEART_RATE("Resting heart rate", "bpm", ReferenceRange.of(40, 50, 100, 130)),
    SYSTOLIC("Systolic blood pressure", "mmHg", ReferenceRange.of(70, 90, 129, 180)),
    DIASTOLIC("Diastolic blood pressure", "mmHg", ReferenceRange.of(40, 60, 79, 120)),
    GLUCOSE_FASTING("Blood glucose (fasting)", "mg/dL", ReferenceRange.of(54, 70, 99, 250)),
    GLUCOSE_NON_FASTING("Blood glucose (after meal / random)", "mg/dL", ReferenceRange.of(54, 70, 140, 300)),
    SLEEP("Sleep", "h", ReferenceRange.of(4, 7, 9, null));

    private final String label;
    private final String unit;
    private final ReferenceRange defaults;

    RangeMetric(String label, String unit, ReferenceRange defaults) {
        this.label = label;
        this.unit = unit;
        this.defaults = defaults;
    }

    public String label() {
        return label;
    }

    public String unit() {
        return unit;
    }

    public ReferenceRange defaults() {
        return defaults;
    }
}
