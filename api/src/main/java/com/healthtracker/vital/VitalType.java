package com.healthtracker.vital;

public enum VitalType {
    WEIGHT("kg"),
    BLOOD_PRESSURE("mmHg"),
    HEART_RATE("bpm"),
    BLOOD_GLUCOSE("mg/dL"),
    SLEEP("h");

    private final String unit;

    VitalType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
