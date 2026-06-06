package com.healthtracker.water.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** {@code loggedAt} defaults to now, which is what the quick-add buttons want. */
public record WaterRequest(@NotNull @Min(1) @Max(5000) Integer amountMl, Instant loggedAt) {}
