package com.healthtracker.medication.dto;

import com.healthtracker.medication.Medication;
import com.healthtracker.medication.ScheduleType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MedicationResponse(
        Long id,
        String name,
        String dosage,
        String instructions,
        boolean asNeeded,
        List<LocalTime> times,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        ScheduleType scheduleType,
        Integer intervalDays,
        List<DayOfWeek> daysOfWeek,
        BigDecimal supply,
        BigDecimal doseQuantity,
        int refillWarningDays) {

    /** A daily medication, as the API described them before schedules existed. */
    public MedicationResponse(
            Long id,
            String name,
            String dosage,
            String instructions,
            boolean asNeeded,
            List<LocalTime> times,
            LocalDate startDate,
            LocalDate endDate,
            boolean active) {
        this(id, name, dosage, instructions, asNeeded, times, startDate, endDate, active,
                ScheduleType.DAILY, null, List.of());
    }

    public MedicationResponse(
            Long id,
            String name,
            String dosage,
            String instructions,
            boolean asNeeded,
            List<LocalTime> times,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            ScheduleType scheduleType,
            Integer intervalDays,
            List<DayOfWeek> daysOfWeek) {
        this(id, name, dosage, instructions, asNeeded, times, startDate, endDate, active,
                scheduleType, intervalDays, daysOfWeek, null, BigDecimal.ONE, 7);
    }

    public static MedicationResponse from(Medication m) {
        return new MedicationResponse(
                m.getId(),
                m.getName(),
                m.getDosage(),
                m.getInstructions(),
                m.isAsNeeded(),
                List.copyOf(m.getTimes()),
                m.getStartDate(),
                m.getEndDate(),
                m.isActive(),
                m.getScheduleType(),
                m.getIntervalDays(),
                m.getDaysOfWeek().stream().sorted().toList(),
                m.getSupply(),
                m.getDoseQuantity(),
                m.getRefillWarningDays());
    }
}
