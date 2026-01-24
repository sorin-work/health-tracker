package com.healthtracker.activity;

import com.healthtracker.activity.dto.ActivityRequest;
import com.healthtracker.activity.dto.ActivityResponse;
import com.healthtracker.activity.dto.WeeklyActivity;
import com.healthtracker.common.PageResponse;
import com.healthtracker.security.AuthUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public PageResponse<ActivityResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return activityService.list(user.id(), from, to, type, page, size);
    }

    @GetMapping("/weekly")
    public List<WeeklyActivity> weekly(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return activityService.weekly(user.id(), from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ActivityRequest request) {
        return activityService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public ActivityResponse update(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return activityService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        activityService.delete(user.id(), id);
    }
}
