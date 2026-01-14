package com.healthtracker.medication;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AdherenceCalculator {

    private AdherenceCalculator() {}

    public static AdherenceStats summarize(Collection<ScheduledDose> doses) {
        int taken = 0;
        int skipped = 0;
        int missed = 0;
        int pending = 0;
        for (ScheduledDose dose : doses) {
            switch (dose.status()) {
                case TAKEN -> taken++;
                case SKIPPED -> skipped++;
                case MISSED -> missed++;
                case PENDING -> pending++;
            }
        }
        int due = taken + skipped + missed;
        Double percent = due == 0 ? null : Math.round(taken * 1000.0 / due) / 10.0;
        return new AdherenceStats(doses.size(), taken, skipped, missed, pending, percent);
    }

    public static Map<Long, AdherenceStats> byMedication(List<ScheduledDose> doses) {
        return group(doses, d -> d.medication().getId(), new LinkedHashMap<>());
    }

    public static Map<LocalDate, AdherenceStats> byDay(List<ScheduledDose> doses) {
        return group(doses, ScheduledDose::day, new TreeMap<>());
    }

    private static <K> Map<K, AdherenceStats> group(
            List<ScheduledDose> doses, Function<ScheduledDose, K> key, Map<K, AdherenceStats> target) {
        Map<K, List<ScheduledDose>> grouped =
                doses.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
        grouped.forEach((k, list) -> target.put(k, summarize(list)));
        return target;
    }
}
