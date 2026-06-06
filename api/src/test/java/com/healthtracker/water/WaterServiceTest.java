package com.healthtracker.water;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.healthtracker.user.UserService;
import com.healthtracker.water.dto.WaterDay;
import com.healthtracker.water.dto.WaterEntry;
import com.healthtracker.water.dto.WaterRequest;
import com.healthtracker.water.dto.WaterTotal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    /** 14:00 in Berlin on 2026-06-10. */
    private static final Instant NOW = Instant.parse("2026-06-10T12:00:00Z");

    @Mock
    WaterRepository water;

    @Mock
    UserService userService;

    WaterService service;

    @BeforeEach
    void setUp() {
        service = new WaterService(water, userService, Clock.fixed(NOW, BERLIN));
        lenient().when(userService.zoneOf(1L)).thenReturn(BERLIN);
        lenient().when(water.save(any(WaterIntake.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static WaterIntake glass(int ml, String at) {
        return new WaterIntake(1L, ml, Instant.parse(at), NOW);
    }

    @Test
    void addDefaultsToNow() {
        WaterEntry entry = service.add(1L, new WaterRequest(250, null));

        assertThat(entry.loggedAt()).isEqualTo(NOW);
        assertThat(entry.amountMl()).isEqualTo(250);
    }

    @Test
    void rejectsEntriesInTheFuture() {
        assertThatThrownBy(() -> service.add(1L, new WaterRequest(250, NOW.plusSeconds(7200))))
                .hasMessageContaining("future");
    }

    @Test
    void todayCoversTheLocalDay() {
        when(water.findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
                        1L, Instant.parse("2026-06-09T22:00:00Z"), Instant.parse("2026-06-10T22:00:00Z")))
                .thenReturn(List.of(glass(250, "2026-06-10T06:00:00Z"), glass(500, "2026-06-10T10:00:00Z")));

        WaterDay today = service.day(1L, null);

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(today.totalMl()).isEqualTo(750);
        assertThat(today.entries()).hasSize(2);
    }

    @Test
    void dailyTotalsFillEmptyDays() {
        when(water.findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(
                        glass(300, "2026-06-08T07:00:00Z"),
                        // 23:30 UTC on the 8th is the 9th in Berlin.
                        glass(200, "2026-06-08T22:30:00Z"),
                        glass(400, "2026-06-10T09:00:00Z")));

        List<WaterTotal> days = service.daily(1L, LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 10));

        assertThat(days).extracting(WaterTotal::totalMl).containsExactly(300, 200, 400);
    }

    @Test
    void dailyRangeIsLimited() {
        assertThatThrownBy(() -> service.daily(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1)))
                .hasMessageContaining("at most");
    }
}
