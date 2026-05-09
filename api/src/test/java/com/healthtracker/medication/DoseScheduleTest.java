package com.healthtracker.medication;

import static com.healthtracker.medication.MedicationFixtures.asNeeded;
import static com.healthtracker.medication.MedicationFixtures.log;
import static com.healthtracker.medication.MedicationFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DoseScheduleTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final LocalDate JAN_10 = LocalDate.of(2026, 1, 10);

    @Test
    void slotsAreInTheUsersLocalTime() {
        Medication m = scheduled(1, "Metformin", JAN_10, null, "20:00", "08:00");

        assertThat(DoseSchedule.slotsOn(m, JAN_10, BERLIN)).containsExactly(
                Instant.parse("2026-01-10T07:00:00Z"), Instant.parse("2026-01-10T19:00:00Z"));
    }

    @Test
    void handlesDaylightSavingChanges() {
        Medication m = scheduled(1, "Metformin", LocalDate.of(2026, 1, 1), null, "08:00");

        assertThat(DoseSchedule.slotsOn(m, LocalDate.of(2026, 3, 28), BERLIN))
                .containsExactly(Instant.parse("2026-03-28T07:00:00Z"));
        assertThat(DoseSchedule.slotsOn(m, LocalDate.of(2026, 3, 30), BERLIN))
                .containsExactly(Instant.parse("2026-03-30T06:00:00Z"));
    }

    @Test
    void respectsStartAndEndDatesInclusively() {
        Medication m = scheduled(1, "Amoxicillin", JAN_10, JAN_10.plusDays(6), "08:00");

        assertThat(DoseSchedule.slotsOn(m, JAN_10.minusDays(1), BERLIN)).isEmpty();
        assertThat(DoseSchedule.slotsOn(m, JAN_10, BERLIN)).hasSize(1);
        assertThat(DoseSchedule.slotsOn(m, JAN_10.plusDays(6), BERLIN)).hasSize(1);
        assertThat(DoseSchedule.slotsOn(m, JAN_10.plusDays(7), BERLIN)).isEmpty();
    }

    @Test
    void asNeededMedicationsHaveNoSlots() {
        assertThat(DoseSchedule.slotsOn(asNeeded(1, "Ibuprofen", JAN_10), JAN_10, BERLIN)).isEmpty();
    }

    @Test
    void expandJoinsLogsAndWorksOutStatus() {
        Medication m = scheduled(1, "Lisinopril", JAN_10, null, "08:00");
        Instant day1 = Instant.parse("2026-01-10T07:00:00Z");
        Instant day2 = Instant.parse("2026-01-11T07:00:00Z");
        List<DoseLog> logs = List.of(log(1, m, day1, DoseStatus.TAKEN), log(2, m, day2, DoseStatus.SKIPPED));

        List<ScheduledDose> doses =
                DoseSchedule.expand(List.of(m), logs, JAN_10, JAN_10.plusDays(3), BERLIN, JAN_10.plusDays(3));

        assertThat(doses).extracting(ScheduledDose::status).containsExactly(
                DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED, DoseStatus.PENDING);
        assertThat(doses.get(0).log().getId()).isEqualTo(1L);
    }

    @Test
    void todaysUnloggedDosesArePendingEvenIfTheirTimeHasPassed() {
        assertThat(DoseSchedule.statusOf(null, JAN_10, JAN_10)).isEqualTo(DoseStatus.PENDING);
        assertThat(DoseSchedule.statusOf(null, JAN_10.minusDays(1), JAN_10)).isEqualTo(DoseStatus.MISSED);
        assertThat(DoseSchedule.statusOf(null, JAN_10.plusDays(1), JAN_10)).isEqualTo(DoseStatus.PENDING);
    }

    @Test
    void expandOrdersDosesByTimeThenName() {
        Medication b = scheduled(2, "B", JAN_10, null, "08:00");
        Medication a = scheduled(1, "A", JAN_10, null, "08:00", "21:00");

        List<ScheduledDose> doses = DoseSchedule.expand(List.of(b, a), List.of(), JAN_10, JAN_10, BERLIN, JAN_10);

        assertThat(doses).extracting(d -> d.medication().getName()).containsExactly("A", "B", "A");
    }
}
