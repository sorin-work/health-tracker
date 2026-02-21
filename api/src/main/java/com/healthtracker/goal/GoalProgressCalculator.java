package com.healthtracker.goal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class GoalProgressCalculator {

    /** How far behind the straight line to the target date still counts as on track. */
    static final double ON_TRACK_TOLERANCE = 5.0;

    private GoalProgressCalculator() {}

    public static GoalProgress progress(Goal goal, GoalMetrics metrics, LocalDate today) {
        BigDecimal current = metrics.valueFor(goal.getType());
        if (current == null) {
            return new GoalProgress(null, 0, false, null);
        }
        return goal.getType() == GoalType.WEIGHT
                ? weightProgress(goal, current, today)
                : rateProgress(goal, current);
    }

    /** Weight goals measure how much of the distance from start to target is covered. */
    private static GoalProgress weightProgress(Goal goal, BigDecimal current, LocalDate today) {
        BigDecimal start = goal.getStartValue() != null ? goal.getStartValue() : current;
        BigDecimal target = goal.getTargetValue();
        boolean losing = target.compareTo(start) < 0;
        boolean achieved = losing ? current.compareTo(target) <= 0 : current.compareTo(target) >= 0;

        BigDecimal distance = start.subtract(target).abs();
        double percent;
        if (achieved) {
            percent = 100;
        } else if (distance.signum() == 0) {
            percent = 0;
        } else {
            BigDecimal covered = losing ? start.subtract(current) : current.subtract(start);
            percent = clamp(covered.divide(distance, MathContext.DECIMAL64).doubleValue() * 100);
        }
        return new GoalProgress(current, round(percent), achieved, achieved ? null : onTrack(goal, percent, today));
    }

    /** Rolling goals (steps, minutes, sleep) are simply current / target. */
    private static GoalProgress rateProgress(Goal goal, BigDecimal current) {
        BigDecimal target = goal.getTargetValue();
        boolean achieved = current.compareTo(target) >= 0;
        double percent = target.signum() == 0
                ? 100
                : clamp(current.divide(target, MathContext.DECIMAL64).doubleValue() * 100);
        return new GoalProgress(current, round(percent), achieved, null);
    }

    static Boolean onTrack(Goal goal, double percent, LocalDate today) {
        if (goal.getTargetDate() == null) {
            return null;
        }
        long total = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getTargetDate());
        if (total <= 0 || !today.isBefore(goal.getTargetDate())) {
            return false;
        }
        long elapsed = Math.max(0, ChronoUnit.DAYS.between(goal.getStartDate(), today));
        double expected = elapsed * 100.0 / total;
        return percent + ON_TRACK_TOLERANCE >= expected;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
