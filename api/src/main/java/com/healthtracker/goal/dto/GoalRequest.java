package com.healthtracker.goal.dto;

import com.healthtracker.goal.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotNull GoalType type,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal targetValue,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal startValue,
        LocalDate targetDate,
        @Size(max = 500) String notes) {}
