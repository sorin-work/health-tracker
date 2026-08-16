package com.healthtracker.medication.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Units added to the supply, e.g. a new pack of 30 tablets. */
public record RefillRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("10000") BigDecimal quantity) {}
