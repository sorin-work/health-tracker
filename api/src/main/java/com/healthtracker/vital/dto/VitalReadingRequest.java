package com.healthtracker.vital.dto;

import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record VitalReadingRequest(
        @NotNull VitalType type,
        @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal value,
        @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal secondaryValue,
        GlucoseContext glucoseContext,
        @NotNull Instant measuredAt,
        @Size(max = 500) String notes) {}
