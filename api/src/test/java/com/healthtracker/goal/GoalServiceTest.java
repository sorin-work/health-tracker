package com.healthtracker.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.healthtracker.activity.Activity;
import com.healthtracker.activity.ActivityRepository;
import com.healthtracker.activity.ActivityType;
import com.healthtracker.common.ApiException;
import com.healthtracker.goal.dto.GoalRequest;
import com.healthtracker.goal.dto.GoalResponse;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-15T12:00:00Z");

    @Mock
    GoalRepository goals;

    @Mock
    VitalReadingRepository readings;

    @Mock
    ActivityRepository activities;

    @Mock
    UserService userService;

    GoalService service;

    @BeforeEach
    void setUp() {
        service = new GoalService(goals, readings, activities, userService, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);
    }

    private void latestWeight(String kg) {
        VitalReading r = new VitalReading(1L, VitalType.WEIGHT, NOW);
        r.setValue(new BigDecimal(kg));
        r.setMeasuredAt(NOW);
        when(readings.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(1L, VitalType.WEIGHT)).thenReturn(Optional.of(r));
    }

    private static Activity walk(int minutes, int steps) {
        Activity a = new Activity(1L, NOW);
        a.setType(ActivityType.WALK);
        a.setStartedAt(NOW.minusSeconds(3600));
        a.setDurationMinutes(minutes);
        a.setSteps(steps);
        return a;
    }

    @Test
    void weightGoalStartsFromTheLatestWeight() {
        latestWeight("88.0");
        when(goals.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = service.create(
                1L, new GoalRequest(GoalType.WEIGHT, new BigDecimal("80"), null, LocalDate.of(2026, 9, 1), null));

        assertThat(response.startValue()).isEqualByComparingTo("88.0");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(response.progressPercent()).isZero();
        assertThat(response.unit()).isEqualTo("kg");
    }

    @Test
    void weightGoalNeedsAStartingWeight() {
        when(readings.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(1L, VitalType.WEIGHT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, new GoalRequest(GoalType.WEIGHT, new BigDecimal("80"), null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Log your weight first");
    }

    @Test
    void targetDateMustBeInTheFuture() {
        assertThatThrownBy(() -> service.create(1L, new GoalRequest(
                        GoalType.DAILY_STEPS, new BigDecimal("8000"), null, LocalDate.of(2026, 4, 15), null)))
                .hasMessageContaining("future");
    }

    @Test
    void stepsAndMinutesComeFromTheLastSevenDays() {
        when(activities.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        eq(1L), eq(Instant.parse("2026-04-09T00:00:00Z")), eq(Instant.parse("2026-04-16T00:00:00Z"))))
                .thenReturn(List.of(walk(60, 28000), walk(30, 14000)));

        GoalMetrics metrics = service.metrics(1L, ZoneOffset.UTC, LocalDate.of(2026, 4, 15));

        assertThat(metrics.averageDailySteps7d()).isEqualByComparingTo("6000");
        assertThat(metrics.activeMinutes7d()).isEqualByComparingTo("90");
        assertThat(metrics.workouts7d()).isEqualByComparingTo("2");
        assertThat(metrics.averageSleep7d()).isNull();
    }

    @Test
    void listStampsGoalsWhenTheyAreAchieved() {
        Goal goal = new Goal(1L, GoalType.WEEKLY_WORKOUTS, LocalDate.of(2026, 4, 1), NOW);
        goal.setTargetValue(new BigDecimal("2"));
        when(goals.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(goal));
        when(activities.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        eq(1L), any(), any()))
                .thenReturn(List.of(walk(30, 3000), walk(30, 3000)));

        List<GoalResponse> list = service.list(1L, false);

        assertThat(list).singleElement().satisfies(g -> {
            assertThat(g.achieved()).isTrue();
            assertThat(g.achievedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void typeCannotChangeOnUpdate() {
        Goal goal = new Goal(1L, GoalType.DAILY_STEPS, LocalDate.of(2026, 4, 1), NOW);
        when(goals.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> service.update(
                        1L, 3L, new GoalRequest(GoalType.SLEEP_HOURS, new BigDecimal("8"), null, null, null)))
                .hasMessageContaining("cannot be changed");
    }

    private static VitalReading sleep(String hours, String wokeUpAt) {
        VitalReading r = new VitalReading(1L, VitalType.SLEEP, NOW);
        r.setValue(new BigDecimal(hours));
        r.setMeasuredAt(Instant.parse(wokeUpAt));
        return r;
    }

    @Test
    void sleepIsAveragedPerNightSoNapsDoNotDragItDown() {
        when(readings.findInRange(eq(1L), eq(VitalType.SLEEP), any(), any())).thenReturn(List.of(
                sleep("7.0", "2026-04-14T06:30:00Z"),
                sleep("1.0", "2026-04-14T15:00:00Z"),
                sleep("6.0", "2026-04-15T06:30:00Z")));

        GoalMetrics metrics = service.metrics(1L, ZoneOffset.UTC, LocalDate.of(2026, 4, 15));

        // (7 + 1) and 6 hours over two nights, not 14 hours over three entries.
        assertThat(metrics.averageSleep7d()).isEqualByComparingTo("7.0");
    }
}
