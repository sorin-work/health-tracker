package com.healthtracker.mood.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MoodRequest(
        @NotNull @Min(1) @Max(5) Integer mood,
        @Min(1) @Max(5) Integer energy,
        @NotNull Instant recordedAt,
        @Size(max = 500) String note) {}
