package com.healthtracker.mood;

import com.healthtracker.mood.dto.MoodDay;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class MoodStats {

    private MoodStats() {}

    /** One point per day that has check-ins, oldest first. */
    public static List<MoodDay> daily(Collection<MoodEntry> entries, ZoneId zone) {
        Map<LocalDate, List<MoodEntry>> byDay = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> LocalDate.ofInstant(e.getRecordedAt(), zone), TreeMap::new, Collectors.toList()));
        List<MoodDay> days = new ArrayList<>(byDay.size());
        byDay.forEach((day, list) -> days.add(new MoodDay(
                day,
                average(list.stream().map(MoodEntry::getMood).toList()),
                average(list.stream().map(MoodEntry::getEnergy).filter(Objects::nonNull).toList()),
                list.size())));
        return days;
    }

    static BigDecimal average(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        int sum = values.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }
}
