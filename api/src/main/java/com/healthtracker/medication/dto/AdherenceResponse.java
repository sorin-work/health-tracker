package com.healthtracker.medication.dto;

import com.healthtracker.medication.AdherenceStats;
import java.time.LocalDate;
import java.util.List;

public record AdherenceResponse(
        LocalDate from,
        LocalDate to,
        AdherenceStats overall,
        List<MedicationAdherence> medications,
        List<DailyAdherence> daily) {

    public record MedicationAdherence(Long medicationId, String name, AdherenceStats stats) {}

    public record DailyAdherence(LocalDate date, AdherenceStats stats) {}
}
