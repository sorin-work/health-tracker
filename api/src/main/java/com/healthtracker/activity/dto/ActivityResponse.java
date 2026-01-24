package com.healthtracker.activity.dto;

import com.healthtracker.activity.Activity;
import com.healthtracker.activity.ActivityType;
import java.math.BigDecimal;
import java.time.Instant;

public record ActivityResponse(
        Long id,
        ActivityType type,
        Instant startedAt,
        int durationMinutes,
        BigDecimal distanceKm,
        Integer steps,
        Integer caloriesKcal,
        String notes) {

    public static ActivityResponse from(Activity a) {
        return new ActivityResponse(
                a.getId(),
                a.getType(),
                a.getStartedAt(),
                a.getDurationMinutes(),
                a.getDistanceKm(),
                a.getSteps(),
                a.getCaloriesKcal(),
                a.getNotes());
    }
}
