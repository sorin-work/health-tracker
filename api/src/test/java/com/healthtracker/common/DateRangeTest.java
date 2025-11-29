package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    void endIsTheStartOfTheDayAfterTo() {
        DateRange range = DateRange.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), ZoneId.of("Europe/Berlin"));

        assertThat(range.start()).isEqualTo(Instant.parse("2026-02-28T23:00:00Z"));
        // DST starts on the 29th, so the end is at +02:00.
        assertThat(range.end()).isEqualTo(Instant.parse("2026-03-31T22:00:00Z"));
        assertThat(range.contains(LocalDate.of(2026, 3, 31))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 4, 1))).isFalse();
    }

    @Test
    void openEndedRangesAreAllowed() {
        DateRange range = DateRange.of(null, null, ZoneId.of("UTC"));

        assertThat(range.from()).isBefore(LocalDate.of(2000, 1, 1));
        assertThat(range.to()).isAfter(LocalDate.of(2100, 1, 1));
    }

    @Test
    void rejectsInvertedRanges() {
        assertThatThrownBy(() -> DateRange.of(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), ZoneId.of("UTC")))
                .isInstanceOf(ApiException.class);
    }
}
