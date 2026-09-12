package com.healthtracker.symptom.dto;

import com.healthtracker.symptom.Symptom;
import java.time.Instant;
import java.util.List;

public record SymptomResponse(
        Long id,
        String name,
        int severity,
        Instant occurredAt,
        Integer durationMinutes,
        String notes,
        List<String> tags) {

    public SymptomResponse(
            Long id, String name, int severity, Instant occurredAt, Integer durationMinutes, String notes) {
        this(id, name, severity, occurredAt, durationMinutes, notes, List.of());
    }

    public static SymptomResponse from(Symptom s) {
        return new SymptomResponse(
                s.getId(),
                s.getName(),
                s.getSeverity(),
                s.getOccurredAt(),
                s.getDurationMinutes(),
                s.getNotes(),
                List.copyOf(s.getTags()));
    }
}
