package com.healthtracker.range.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateRangeRequest(
        @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal low,
        @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal high) {}
