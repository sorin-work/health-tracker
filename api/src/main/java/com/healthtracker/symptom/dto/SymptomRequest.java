package com.healthtracker.symptom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record SymptomRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @Min(1) @Max(10) Integer severity,
        @NotNull Instant occurredAt,
        @PositiveOrZero @Max(60 * 24 * 14) Integer durationMinutes,
        @Size(max = 500) String notes,
        @Size(max = 10, message = "at most 10 tags") List<@NotBlank @Size(max = 30) String> tags) {

    public SymptomRequest(String name, Integer severity, Instant occurredAt, Integer durationMinutes, String notes) {
        this(name, severity, occurredAt, durationMinutes, notes, null);
    }
}
