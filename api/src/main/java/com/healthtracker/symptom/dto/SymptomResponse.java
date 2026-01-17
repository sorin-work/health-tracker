package com.healthtracker.symptom.dto;

import com.healthtracker.symptom.Symptom;
import java.time.Instant;

public record SymptomResponse(
        Long id, String name, int severity, Instant occurredAt, Integer durationMinutes, String notes) {

    public static SymptomResponse from(Symptom s) {
        return new SymptomResponse(
                s.getId(), s.getName(), s.getSeverity(), s.getOccurredAt(), s.getDurationMinutes(), s.getNotes());
    }
}
