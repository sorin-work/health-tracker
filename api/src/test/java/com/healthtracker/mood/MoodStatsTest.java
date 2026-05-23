package com.healthtracker.mood;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.mood.dto.MoodDay;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoodStatsTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static MoodEntry entry(int mood, Integer energy, String at) {
        MoodEntry e = new MoodEntry(1L, Instant.EPOCH);
        e.setMood(mood);
        e.setEnergy(energy);
        e.setRecordedAt(Instant.parse(at));
        return e;
    }

    @Test
    void averagesEachDayInTheUsersZone() {
        List<MoodDay> days = MoodStats.daily(List.of(
                entry(4, 3, "2026-05-20T07:00:00Z"),
                entry(2, null, "2026-05-20T19:00:00Z"),
                // 23:30 UTC is already the next day in Berlin.
                entry(5, 5, "2026-05-20T23:30:00Z")), BERLIN);

        assertThat(days).extracting(MoodDay::date)
                .containsExactly(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 21));
        assertThat(days.get(0).mood()).isEqualByComparingTo("3.0");
        assertThat(days.get(0).energy()).isEqualByComparingTo("3.0");
        assertThat(days.get(0).count()).isEqualTo(2);
        assertThat(days.get(1).mood()).isEqualByComparingTo("5.0");
    }

    @Test
    void energyIsEmptyWhenNeverLogged() {
        List<MoodDay> days = MoodStats.daily(List.of(entry(3, null, "2026-05-20T07:00:00Z")), BERLIN);

        assertThat(days).singleElement().satisfies(d -> assertThat(d.energy()).isNull());
    }
}
