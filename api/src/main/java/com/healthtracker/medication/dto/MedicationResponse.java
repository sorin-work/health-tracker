package com.healthtracker.medication.dto;

import com.healthtracker.medication.Medication;
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
        boolean active) {

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
                m.isActive());
    }
}
