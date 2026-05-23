package com.healthtracker.mood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.mood.dto.MoodRequest;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-20T12:00:00Z");

    @Mock
    MoodRepository moods;

    @Mock
    UserService userService;

    MoodService service;

    @BeforeEach
    void setUp() {
        service = new MoodService(moods, userService, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);
        lenient().when(moods.save(any(MoodEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsACheckInAndTrimsTheNote() {
        MoodResponse created = service.create(1L, new MoodRequest(4, 2, NOW, "  slept badly  "));

        assertThat(created.mood()).isEqualTo(4);
        assertThat(created.energy()).isEqualTo(2);
        assertThat(created.note()).isEqualTo("slept badly");
    }

    @Test
    void blankNotesAreDropped() {
        assertThat(service.create(1L, new MoodRequest(3, null, NOW, "   ")).note()).isNull();
    }

    @Test
    void rejectsCheckInsInTheFuture() {
        assertThatThrownBy(() -> service.create(1L, new MoodRequest(3, null, NOW.plusSeconds(3600), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("future");
    }

    @Test
    void cannotTouchAnotherUsersEntry() {
        when(moods.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 5L)).hasMessageContaining("not found");
    }

    @Test
    void dailyDefaultsToTheLast30Days() {
        service.daily(1L, null, null);

        verify(moods).findByUserIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
                eq(1L), eq(Instant.parse("2026-04-21T00:00:00Z")), eq(Instant.parse("2026-05-21T00:00:00Z")));
    }
}
