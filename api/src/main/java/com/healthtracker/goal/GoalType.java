package com.healthtracker.goal;

public enum GoalType {
    /** Reach a body weight (kg), up or down from the starting weight. */
    WEIGHT("kg"),
    /** Average daily steps over the last 7 days. */
    DAILY_STEPS("steps"),
    /** Active minutes in the last 7 days. */
    WEEKLY_ACTIVE_MINUTES("min"),
    /** Workouts in the last 7 days. */
    WEEKLY_WORKOUTS("sessions"),
    /** Average nightly sleep over the last 7 days. */
    SLEEP_HOURS("h");

    private final String unit;

    GoalType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
