package com.healthtracker.goal;

import java.math.BigDecimal;

/** The user's current numbers that goals are measured against. Any of them may be null. */
public record GoalMetrics(
        BigDecimal latestWeight,
        BigDecimal averageDailySteps7d,
        BigDecimal activeMinutes7d,
        BigDecimal workouts7d,
        BigDecimal averageSleep7d) {

    public BigDecimal valueFor(GoalType type) {
        return switch (type) {
            case WEIGHT -> latestWeight;
            case DAILY_STEPS -> averageDailySteps7d;
            case WEEKLY_ACTIVE_MINUTES -> activeMinutes7d;
            case WEEKLY_WORKOUTS -> workouts7d;
            case SLEEP_HOURS -> averageSleep7d;
        };
    }
}
