package com.healthtracker.goal;

import com.healthtracker.activity.Activity;
import com.healthtracker.activity.ActivityRepository;
import com.healthtracker.common.ApiException;
import com.healthtracker.goal.dto.GoalRequest;
import com.healthtracker.goal.dto.GoalResponse;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private static final int WINDOW_DAYS = 7;

    private final GoalRepository goals;
    private final VitalReadingRepository readings;
    private final ActivityRepository activities;
    private final UserService userService;
    private final Clock clock;

    public GoalService(
            GoalRepository goals,
            VitalReadingRepository readings,
            ActivityRepository activities,
            UserService userService,
            Clock clock) {
        this.goals = goals;
        this.readings = readings;
        this.activities = activities;
        this.userService = userService;
        this.clock = clock;
    }

    /** Lists goals with live progress, and stamps goals that were reached since last time. */
    @Transactional
    public List<GoalResponse> list(Long userId, boolean includeArchived) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        GoalMetrics metrics = metrics(userId, zone, today);
        return goals.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(g -> includeArchived || !g.isArchived())
                .map(g -> toResponse(g, metrics, today))
                .toList();
    }

    @Transactional
    public GoalResponse create(Long userId, GoalRequest request) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        GoalMetrics metrics = metrics(userId, zone, today);
        validateDates(today, request.targetDate());

        Goal goal = new Goal(userId, request.type(), today, clock.instant());
        goal.setTargetValue(request.targetValue());
        if (request.type() == GoalType.WEIGHT) {
            BigDecimal start = request.startValue() != null ? request.startValue() : metrics.latestWeight();
            if (start == null) {
                throw ApiException.badRequest("Log your weight first, or enter a starting weight");
            }
            if (start.compareTo(request.targetValue()) == 0) {
                throw ApiException.badRequest("Target weight is the same as the starting weight");
            }
            goal.setStartValue(start);
        }
        goal.setTargetDate(request.targetDate());
        goal.setNotes(trimToNull(request.notes()));
        return toResponse(goals.save(goal), metrics, today);
    }

    @Transactional
    public GoalResponse update(Long userId, Long id, GoalRequest request) {
        Goal goal = find(userId, id);
        if (goal.getType() != request.type()) {
            throw ApiException.badRequest("A goal's type cannot be changed; create a new goal instead");
        }
        ZoneId zone = userService.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        validateDates(goal.getStartDate(), request.targetDate());
        goal.setTargetValue(request.targetValue());
        if (goal.getType() == GoalType.WEIGHT && request.startValue() != null) {
            goal.setStartValue(request.startValue());
        }
        goal.setTargetDate(request.targetDate());
        goal.setNotes(trimToNull(request.notes()));
        goal.setAchievedAt(null);
        return toResponse(goal, metrics(userId, zone, today), today);
    }

    @Transactional
    public GoalResponse setArchived(Long userId, Long id, boolean archived) {
        Goal goal = find(userId, id);
        goal.setArchived(archived);
        ZoneId zone = userService.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return toResponse(goal, metrics(userId, zone, today), today);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        goals.delete(find(userId, id));
    }

    GoalMetrics metrics(Long userId, ZoneId zone, LocalDate today) {
        Instant windowStart = today.minusDays(WINDOW_DAYS - 1).atStartOfDay(zone).toInstant();
        Instant windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal weight = readings.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(userId, VitalType.WEIGHT)
                .map(VitalReading::getValue)
                .orElse(null);

        List<Activity> recent = activities
                .findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        userId, windowStart, windowEnd);
        long totalSteps = recent.stream().mapToLong(a -> a.getSteps() == null ? 0 : a.getSteps()).sum();
        BigDecimal steps =
                BigDecimal.valueOf(totalSteps).divide(BigDecimal.valueOf(WINDOW_DAYS), 0, RoundingMode.HALF_UP);
        BigDecimal minutes = BigDecimal.valueOf(recent.stream().mapToInt(Activity::getDurationMinutes).sum());
        BigDecimal workouts = BigDecimal.valueOf(recent.size());

        List<VitalReading> sleep = readings.findInRange(userId, VitalType.SLEEP, windowStart, windowEnd);
        BigDecimal avgSleep = sleep.isEmpty()
                ? null
                : sleep.stream()
                        .map(VitalReading::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(sleep.size()), 1, RoundingMode.HALF_UP);

        return new GoalMetrics(weight, steps, minutes, workouts, avgSleep);
    }

    private GoalResponse toResponse(Goal goal, GoalMetrics metrics, LocalDate today) {
        GoalProgress progress = GoalProgressCalculator.progress(goal, metrics, today);
        if (progress.achieved() && goal.getAchievedAt() == null && !goal.isArchived()) {
            goal.setAchievedAt(clock.instant());
        }
        return GoalResponse.from(goal, progress);
    }

    private Goal find(Long userId, Long id) {
        return goals.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Goal"));
    }

    private static void validateDates(LocalDate start, LocalDate targetDate) {
        if (targetDate != null && !targetDate.isAfter(start)) {
            throw ApiException.badRequest("Target date must be in the future");
        }
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
