package com.healthtracker.activity.dto;

import com.healthtracker.activity.ActivityType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record ActivityRequest(
        @NotNull ActivityType type,
        @NotNull Instant startedAt,
        @NotNull @Min(1) @Max(24 * 60) Integer durationMinutes,
        @DecimalMin("0") @DecimalMax("1000") BigDecimal distanceKm,
        @PositiveOrZero @Max(200_000) Integer steps,
        @PositiveOrZero @Max(20_000) Integer caloriesKcal,
        @Size(max = 500) String notes) {}
