package com.healthtracker.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Expands medication schedules into concrete dose slots and works out their status. */
public final class DoseSchedule {

    private DoseSchedule() {}

    /** Instants a medication is due on a given local day. Empty for as-needed medications. */
    public static List<Instant> slotsOn(Medication medication, LocalDate day, ZoneId zone) {
        if (!medication.isScheduledOn(day)) {
            return List.of();
        }
        List<Instant> slots = new ArrayList<>(medication.getTimes().size());
        for (LocalTime time : medication.getTimes()) {
            slots.add(day.atTime(time).atZone(zone).toInstant());
        }
        slots.sort(Comparator.naturalOrder());
        return slots;
    }

    /**
     * Every slot of every medication between {@code from} and {@code to} (inclusive), joined with the
     * logs and given a status as of {@code today}.
     */
    public static List<ScheduledDose> expand(
            Collection<Medication> medications,
            Collection<DoseLog> logs,
            LocalDate from,
            LocalDate to,
            ZoneId zone,
            LocalDate today) {
        Map<String, DoseLog> logsBySlot = new HashMap<>();
        for (DoseLog log : logs) {
            logsBySlot.put(key(log.getMedicationId(), log.getScheduledAt()), log);
        }
        List<ScheduledDose> doses = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            for (Medication medication : medications) {
                for (Instant slot : slotsOn(medication, day, zone)) {
                    DoseLog log = logsBySlot.get(key(medication.getId(), slot));
                    doses.add(new ScheduledDose(medication, day, slot, log, statusOf(log, day, today)));
                }
            }
        }
        doses.sort(Comparator.comparing(ScheduledDose::scheduledAt)
                .thenComparing(d -> d.medication().getName()));
        return doses;
    }

    /**
     * A logged dose keeps its logged status. An unlogged dose only counts as missed once its day is
     * over, so this morning's dose is still pending in the evening.
     */
    public static DoseStatus statusOf(DoseLog log, LocalDate day, LocalDate today) {
        if (log != null) {
            return log.getStatus();
        }
        return day.isBefore(today) ? DoseStatus.MISSED : DoseStatus.PENDING;
    }

    private static String key(Long medicationId, Instant slot) {
        return medicationId + "@" + slot.getEpochSecond();
    }
}
