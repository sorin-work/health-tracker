package com.healthtracker.activity;

import com.healthtracker.activity.dto.ActivityRequest;
import com.healthtracker.activity.dto.ActivityResponse;
import com.healthtracker.activity.dto.WeeklyActivity;
import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activities;
    private final UserService userService;
    private final Clock clock;

    public ActivityService(ActivityRepository activities, UserService userService, Clock clock) {
        this.activities = activities;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(
            Long userId, LocalDate from, LocalDate to, ActivityType type, int page, int size) {
        return PageResponse.of(entries(userId, from, to, type), page, size);
    }

    /** Activities in a date range, newest first. */
    @Transactional(readOnly = true)
    public List<ActivityResponse> entries(Long userId, LocalDate from, LocalDate to, ActivityType type) {
        DateRange range = DateRange.of(from, to, userService.zoneOf(userId));
        return inRange(userId, range).stream()
                .filter(a -> type == null || a.getType() == type)
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WeeklyActivity> weekly(Long userId, LocalDate from, LocalDate to) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(zone));
        LocalDate start = from != null ? from : end.minusWeeks(11);
        DateRange range = DateRange.of(ActivityStats.weekStart(start), end, zone);
        return ActivityStats.weekly(inRange(userId, range), range.from(), end, zone);
    }

    @Transactional
    public ActivityResponse create(Long userId, ActivityRequest request) {
        Activity activity = new Activity(userId, clock.instant());
        apply(activity, request);
        return ActivityResponse.from(activities.save(activity));
    }

    @Transactional
    public ActivityResponse update(Long userId, Long id, ActivityRequest request) {
        Activity activity = find(userId, id);
        apply(activity, request);
        return ActivityResponse.from(activity);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        activities.delete(find(userId, id));
    }

    List<Activity> inRange(Long userId, DateRange range) {
        return activities.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                userId, range.start(), range.end());
    }

    private Activity find(Long userId, Long id) {
        return activities.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Activity"));
    }

    private void apply(Activity activity, ActivityRequest request) {
        if (request.startedAt().isAfter(clock.instant().plusSeconds(600))) {
            throw ApiException.badRequest("Activities cannot start in the future");
        }
        activity.setType(request.type());
        activity.setStartedAt(request.startedAt());
        activity.setDurationMinutes(request.durationMinutes());
        activity.setDistanceKm(request.distanceKm());
        activity.setSteps(request.steps());
        activity.setCaloriesKcal(request.caloriesKcal());
        activity.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }
}
