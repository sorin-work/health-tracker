package com.healthtracker.report;

import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.medication.AdherenceStats;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingResponse;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Everything a doctor wants to see about a period, in one response, ready to print. */
public record DoctorReport(
        String patient,
        String timezone,
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        List<VitalStats> vitals,
        List<VitalReadingResponse> flaggedReadings,
        int flaggedTotal,
        AdherenceStats adherence,
        List<MedicationLine> medications,
        List<SymptomStats> symptoms,
        MoodStats mood,
        ActivityStats activity,
        List<JournalResponse> notes) {

    public record VitalStats(
            VitalType type,
            String unit,
            int count,
            BigDecimal average,
            BigDecimal averageSecondary,
            BigDecimal min,
            BigDecimal max,
            int flagged) {}

    public record MedicationLine(
            String name,
            String dosage,
            String schedule,
            List<LocalTime> times,
            Integer intervalDays,
            List<DayOfWeek> daysOfWeek,
            LocalDate startDate,
            LocalDate endDate,
            boolean asNeeded,
            AdherenceStats adherence,
            int asNeededTaken) {}

    public record SymptomStats(String name, int count, BigDecimal averageSeverity, int maxSeverity) {}

    public record MoodStats(int checkIns, BigDecimal averageMood, BigDecimal averageEnergy) {}

    public record ActivityStats(int sessions, int activeMinutes, BigDecimal averageMinutesPerWeek) {}
}
