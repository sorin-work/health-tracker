package com.healthtracker.medication.dto;

import com.healthtracker.medication.ScheduleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** {@code scheduleType} defaults to daily, so clients that predate schedules keep working. */
public record MedicationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String dosage,
        @Size(max = 500) String instructions,
        boolean asNeeded,
        @Size(max = 12, message = "at most 12 times a day") List<@NotNull LocalTime> times,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        ScheduleType scheduleType,
        @Min(2) @Max(90) Integer intervalDays,
        Set<DayOfWeek> daysOfWeek,
        @DecimalMin("0") @DecimalMax("99999") BigDecimal supply,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal doseQuantity,
        @Min(0) @Max(90) Integer refillWarningDays) {

    public MedicationRequest(
            String name,
            String dosage,
            String instructions,
            boolean asNeeded,
            List<LocalTime> times,
            LocalDate startDate,
            LocalDate endDate) {
        this(name, dosage, instructions, asNeeded, times, startDate, endDate, null, null, null);
    }

    public MedicationRequest(
            String name,
            String dosage,
            String instructions,
            boolean asNeeded,
            List<LocalTime> times,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleType scheduleType,
            Integer intervalDays,
            Set<DayOfWeek> daysOfWeek) {
        this(name, dosage, instructions, asNeeded, times, startDate, endDate, scheduleType, intervalDays, daysOfWeek,
                null, null, null);
    }
}
