package com.healthtracker.activity;

import com.healthtracker.activity.dto.WeeklyActivity;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class ActivityStats {

    private ActivityStats() {}

    public static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Weekly totals between two dates, including empty weeks so charts have no gaps. */
    public static List<WeeklyActivity> weekly(Collection<Activity> activities, LocalDate from, LocalDate to, ZoneId zone) {
        Map<LocalDate, List<Activity>> byWeek = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> weekStart(LocalDate.ofInstant(a.getStartedAt(), zone)), TreeMap::new, Collectors.toList()));
        List<WeeklyActivity> weeks = new ArrayList<>();
        for (LocalDate week = weekStart(from); !week.isAfter(to); week = week.plusWeeks(1)) {
            List<Activity> list = byWeek.getOrDefault(week, List.of());
            weeks.add(new WeeklyActivity(
                    week,
                    list.size(),
                    list.stream().mapToInt(Activity::getDurationMinutes).sum(),
                    list.stream()
                            .map(Activity::getDistanceKm)
                            .filter(d -> d != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    list.stream().mapToLong(a -> a.getSteps() == null ? 0 : a.getSteps()).sum(),
                    list.stream().mapToLong(a -> a.getCaloriesKcal() == null ? 0 : a.getCaloriesKcal()).sum()));
        }
        return weeks;
    }

    /** Steps per local day. Days without any steps are absent. */
    public static Map<LocalDate, Long> stepsByDay(Collection<Activity> activities, ZoneId zone) {
        return activities.stream()
                .filter(a -> a.getSteps() != null)
                .collect(Collectors.groupingBy(
                        a -> LocalDate.ofInstant(a.getStartedAt(), zone),
                        TreeMap::new,
                        Collectors.summingLong(Activity::getSteps)));
    }
}
