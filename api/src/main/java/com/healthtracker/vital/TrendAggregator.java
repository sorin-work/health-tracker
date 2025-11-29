package com.healthtracker.vital;

import com.healthtracker.range.Flag;
import com.healthtracker.range.RangeFlagger;
import com.healthtracker.vital.dto.TrendPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Groups readings into one point per calendar day (in the user's zone) for trend charts. */
public final class TrendAggregator {

    private TrendAggregator() {}

    public static List<TrendPoint> daily(List<VitalReading> readings, ZoneId zone, RangeFlagger flagger) {
        Map<LocalDate, List<VitalReading>> byDay = readings.stream()
                .collect(Collectors.groupingBy(
                        r -> LocalDate.ofInstant(r.getMeasuredAt(), zone), TreeMap::new, Collectors.toList()));

        List<TrendPoint> points = new ArrayList<>(byDay.size());
        byDay.forEach((day, list) -> {
            List<BigDecimal> values = list.stream().map(VitalReading::getValue).toList();
            List<BigDecimal> secondary = list.stream()
                    .map(VitalReading::getSecondaryValue)
                    .filter(Objects::nonNull)
                    .toList();
            int flagged = (int) list.stream()
                    .map(r -> flagger.flag(r.getType(), r.getValue(), r.getSecondaryValue(), r.getGlucoseContext()))
                    .filter(f -> f != null && f.outOfRange())
                    .count();
            points.add(new TrendPoint(
                    day,
                    average(values),
                    values.stream().min(BigDecimal::compareTo).orElse(null),
                    values.stream().max(BigDecimal::compareTo).orElse(null),
                    average(secondary),
                    list.size(),
                    flagged));
        });
        return points;
    }

    static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    static boolean isFlagged(Flag flag) {
        return flag != null && flag.outOfRange();
    }
}
