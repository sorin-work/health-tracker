package com.healthtracker.vital;

/** When a glucose reading was taken. Fasting and pre-meal readings use the stricter range. */
public enum GlucoseContext {
    FASTING(true),
    BEFORE_MEAL(true),
    AFTER_MEAL(false),
    BEDTIME(false),
    RANDOM(false);

    private final boolean fasting;

    GlucoseContext(boolean fasting) {
        this.fasting = fasting;
    }

    public boolean fasting() {
        return fasting;
    }
}
