package com.healthtracker.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

final class MedicationFixtures {

    private MedicationFixtures() {}

    static Medication scheduled(long id, String name, LocalDate start, LocalDate end, String... times) {
        Medication m = new Medication(1L, name, Instant.EPOCH);
        m.setId(id);
        m.setStartDate(start);
        m.setEndDate(end);
        m.setTimes(Arrays.stream(times).map(LocalTime::parse).toList());
        return m;
    }

    static Medication asNeeded(long id, String name, LocalDate start) {
        Medication m = scheduled(id, name, start, null);
        m.setAsNeeded(true);
        return m;
    }

    static DoseLog log(long id, Medication m, Instant slot, DoseStatus status) {
        DoseLog log = new DoseLog(m.getId(), 1L, slot);
        log.setId(id);
        log.record(status, slot.plusSeconds(300), null);
        return log;
    }
}
