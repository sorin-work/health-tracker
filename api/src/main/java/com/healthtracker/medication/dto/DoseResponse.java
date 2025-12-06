package com.healthtracker.medication.dto;

import com.healthtracker.medication.DoseLog;
import com.healthtracker.medication.DoseStatus;
import com.healthtracker.medication.Medication;
import com.healthtracker.medication.ScheduledDose;
import java.time.Instant;

public record DoseResponse(
        Long medicationId,
        String medicationName,
        String dosage,
        boolean asNeeded,
        Instant scheduledAt,
        DoseStatus status,
        Long logId,
        Instant recordedAt,
        String note) {

    public static DoseResponse from(ScheduledDose dose) {
        return of(dose.medication(), dose.scheduledAt(), dose.status(), dose.log());
    }

    public static DoseResponse of(Medication m, Instant scheduledAt, DoseStatus status, DoseLog log) {
        return new DoseResponse(
                m.getId(),
                m.getName(),
                m.getDosage(),
                m.isAsNeeded(),
                scheduledAt,
                status,
                log == null ? null : log.getId(),
                log == null ? null : log.getRecordedAt(),
                log == null ? null : log.getNote());
    }
}
