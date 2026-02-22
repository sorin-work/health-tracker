package com.healthtracker.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GoalProgressCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private static Goal goal(GoalType type, String target, String start, LocalDate targetDate) {
        Goal g = new Goal(1L, type, START, Instant.EPOCH);
        g.setTargetValue(new BigDecimal(target));
        g.setStartValue(start == null ? null : new BigDecimal(start));
        g.setTargetDate(targetDate);
        return g;
    }

    private static GoalMetrics weight(String kg) {
        return new GoalMetrics(new BigDecimal(kg), null, null, null, null);
    }

    @Test
    void weightLossProgressIsTheShareOfDistanceCovered() {
        GoalProgress p = GoalProgressCalculator.progress(goal(GoalType.WEIGHT, "80", "90", null), weight("86"), START);

        assertThat(p.percent()).isEqualTo(40.0);
        assertThat(p.achieved()).isFalse();
        assertThat(p.currentValue()).isEqualByComparingTo("86");
        assertThat(p.onTrack()).isNull();
    }

    @Test
    void weightGainWorksTheOtherWayRound() {
        GoalProgress p = GoalProgressCalculator.progress(goal(GoalType.WEIGHT, "60", "55", null), weight("57.5"), START);

        assertThat(p.percent()).isEqualTo(50.0);
    }

    @Test
    void goingTheWrongWayIsZeroNotNegative() {
        GoalProgress p = GoalProgressCalculator.progress(goal(GoalType.WEIGHT, "80", "90", null), weight("92"), START);

        assertThat(p.percent()).isZero();
    }

    @Test
    void passingTheTargetCountsAsAchieved() {
        GoalProgress p = GoalProgressCalculator.progress(goal(GoalType.WEIGHT, "80", "90", null), weight("79.4"), START);

        assertThat(p.achieved()).isTrue();
        assertThat(p.percent()).isEqualTo(100.0);
    }

    @Test
    void onTrackComparesWithAStraightLineToTheTargetDate() {
        Goal g = goal(GoalType.WEIGHT, "80", "90", START.plusDays(100));
        LocalDate halfway = START.plusDays(50);

        assertThat(GoalProgressCalculator.progress(g, weight("85"), halfway).onTrack()).isTrue();
        assertThat(GoalProgressCalculator.progress(g, weight("85.4"), halfway).onTrack()).isTrue(); // within tolerance
        assertThat(GoalProgressCalculator.progress(g, weight("87"), halfway).onTrack()).isFalse();
    }

    @Test
    void notOnTrackOnceTheTargetDateHasPassed() {
        Goal g = goal(GoalType.WEIGHT, "80", "90", START.plusDays(30));

        assertThat(GoalProgressCalculator.progress(g, weight("82"), START.plusDays(31)).onTrack()).isFalse();
    }

    @Test
    void rollingGoalsAreCurrentOverTarget() {
        GoalMetrics metrics = new GoalMetrics(
                null, new BigDecimal("6000"), new BigDecimal("180"), new BigDecimal("2"), new BigDecimal("7.2"));

        GoalProgress steps = GoalProgressCalculator.progress(goal(GoalType.DAILY_STEPS, "8000", null, null), metrics, START);
        GoalProgress minutes =
                GoalProgressCalculator.progress(goal(GoalType.WEEKLY_ACTIVE_MINUTES, "150", null, null), metrics, START);
        GoalProgress sleep = GoalProgressCalculator.progress(goal(GoalType.SLEEP_HOURS, "8", null, null), metrics, START);

        assertThat(steps.percent()).isEqualTo(75.0);
        assertThat(steps.achieved()).isFalse();
        assertThat(minutes.percent()).isEqualTo(100.0);
        assertThat(minutes.achieved()).isTrue();
        assertThat(sleep.percent()).isEqualTo(90.0);
    }

    @Test
    void noDataMeansNoProgress() {
        GoalProgress p = GoalProgressCalculator.progress(
                goal(GoalType.SLEEP_HOURS, "8", null, null), new GoalMetrics(null, null, null, null, null), START);

        assertThat(p.currentValue()).isNull();
        assertThat(p.percent()).isZero();
        assertThat(p.achieved()).isFalse();
    }
}
