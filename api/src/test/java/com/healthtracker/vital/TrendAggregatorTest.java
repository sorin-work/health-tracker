package com.healthtracker.vital;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.range.RangeFlagger;
import com.healthtracker.vital.dto.TrendPoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendAggregatorTest {

    private static VitalReading bp(String at, int sys, int dia) {
        VitalReading r = new VitalReading(1L, VitalType.BLOOD_PRESSURE, Instant.parse(at));
        r.setValue(BigDecimal.valueOf(sys));
        r.setSecondaryValue(BigDecimal.valueOf(dia));
        r.setMeasuredAt(Instant.parse(at));
        return r;
    }

    @Test
    void averagesEachDayAndCountsFlaggedReadings() {
        List<VitalReading> readings = List.of(
                bp("2026-01-05T07:00:00Z", 120, 78),
                bp("2026-01-05T19:00:00Z", 140, 90),
                bp("2026-01-06T07:00:00Z", 118, 76));

        List<TrendPoint> points = TrendAggregator.daily(readings, ZoneOffset.UTC, RangeFlagger.withDefaults());

        assertThat(points).hasSize(2);
        TrendPoint first = points.get(0);
        assertThat(first.date()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(first.average()).isEqualByComparingTo("130.0");
        assertThat(first.averageSecondary()).isEqualByComparingTo("84.0");
        assertThat(first.min()).isEqualByComparingTo("120");
        assertThat(first.max()).isEqualByComparingTo("140");
        assertThat(first.count()).isEqualTo(2);
        assertThat(first.flagged()).isEqualTo(1);
        assertThat(points.get(1).flagged()).isZero();
    }

    @Test
    void groupsByTheUsersLocalDay() {
        // 23:30 UTC on the 5th is already the 6th in Berlin.
        List<VitalReading> readings = List.of(bp("2026-01-05T23:30:00Z", 120, 78), bp("2026-01-06T07:00:00Z", 124, 80));

        List<TrendPoint> utc = TrendAggregator.daily(readings, ZoneOffset.UTC, RangeFlagger.withDefaults());
        List<TrendPoint> berlin =
                TrendAggregator.daily(readings, ZoneId.of("Europe/Berlin"), RangeFlagger.withDefaults());

        assertThat(utc).hasSize(2);
        assertThat(berlin).hasSize(1);
        assertThat(berlin.get(0).date()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(berlin.get(0).average()).isEqualByComparingTo("122.0");
    }

    @Test
    void emptyInputGivesNoPoints() {
        assertThat(TrendAggregator.daily(List.of(), ZoneOffset.UTC, RangeFlagger.withDefaults())).isEmpty();
    }
}
