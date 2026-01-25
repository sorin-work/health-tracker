package com.healthtracker.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.activity.dto.WeeklyActivity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityStatsTest {

    private static Activity activity(String at, int minutes, Integer steps, String km) {
        Activity a = new Activity(1L, Instant.parse(at));
        a.setType(ActivityType.WALK);
        a.setStartedAt(Instant.parse(at));
        a.setDurationMinutes(minutes);
        a.setSteps(steps);
        a.setDistanceKm(km == null ? null : new BigDecimal(km));
        return a;
    }

    @Test
    void weeksStartOnMonday() {
        assertThat(ActivityStats.weekStart(LocalDate.of(2026, 3, 8))).isEqualTo(LocalDate.of(2026, 3, 2)); // Sunday
        assertThat(ActivityStats.weekStart(LocalDate.of(2026, 3, 9))).isEqualTo(LocalDate.of(2026, 3, 9)); // Monday
    }

    @Test
    void weeklyTotalsIncludeEmptyWeeks() {
        List<Activity> list = List.of(
                activity("2026-03-02T18:00:00Z", 40, 5000, "3.5"),
                activity("2026-03-08T09:00:00Z", 30, null, null),
                activity("2026-03-17T18:00:00Z", 50, 6500, "4.2"));

        List<WeeklyActivity> weeks =
                ActivityStats.weekly(list, LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 20), ZoneOffset.UTC);

        assertThat(weeks).extracting(WeeklyActivity::weekStart).containsExactly(
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16));
        assertThat(weeks.get(0).sessions()).isEqualTo(2);
        assertThat(weeks.get(0).activeMinutes()).isEqualTo(70);
        assertThat(weeks.get(0).steps()).isEqualTo(5000);
        assertThat(weeks.get(0).distanceKm()).isEqualByComparingTo("3.5");
        assertThat(weeks.get(1).sessions()).isZero();
        assertThat(weeks.get(2).distanceKm()).isEqualByComparingTo("4.2");
    }

    @Test
    void stepsAreSummedPerDay() {
        Map<LocalDate, Long> steps = ActivityStats.stepsByDay(List.of(
                activity("2026-03-02T08:00:00Z", 20, 2000, null),
                activity("2026-03-02T18:00:00Z", 40, 5000, null),
                activity("2026-03-03T18:00:00Z", 40, null, null)), ZoneOffset.UTC);

        assertThat(steps).containsExactly(Map.entry(LocalDate.of(2026, 3, 2), 7000L));
    }
}
