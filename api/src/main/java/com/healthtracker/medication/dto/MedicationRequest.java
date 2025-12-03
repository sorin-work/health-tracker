package com.healthtracker.medication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MedicationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String dosage,
        @Size(max = 500) String instructions,
        boolean asNeeded,
        @Size(max = 12, message = "at most 12 times a day") List<@NotNull LocalTime> times,
        @NotNull LocalDate startDate,
        LocalDate endDate) {}
