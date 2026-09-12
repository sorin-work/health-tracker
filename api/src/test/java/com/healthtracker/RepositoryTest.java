package com.healthtracker;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.journal.JournalEntry;
import com.healthtracker.journal.JournalRepository;
import com.healthtracker.medication.Medication;
import com.healthtracker.medication.MedicationRepository;
import com.healthtracker.mood.MoodEntry;
import com.healthtracker.mood.MoodRepository;
import com.healthtracker.symptom.Symptom;
import com.healthtracker.symptom.SymptomRepository;
import com.healthtracker.user.User;
import com.healthtracker.user.UserRepository;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
import com.healthtracker.water.WaterIntake;
import com.healthtracker.water.WaterRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** Checks the derived queries and mappings against an in-memory H2 database. */
@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:repo-test;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired UserRepository users;
    @Autowired VitalReadingRepository readings;
    @Autowired SymptomRepository symptoms;
    @Autowired MedicationRepository medications;
    @Autowired MoodRepository moods;
    @Autowired JournalRepository journal;
    @Autowired WaterRepository water;

    Long userId;
    Long otherUserId;

    @BeforeEach
    void setUp() {
        userId = users.save(new User("sam@example.com", "hash", "Sam", "UTC", Instant.EPOCH)).getId();
        otherUserId = users.save(new User("kim@example.com", "hash", "Kim", "UTC", Instant.EPOCH)).getId();
    }

    private VitalReading reading(Long user, VitalType type, String value, String at) {
        VitalReading r = new VitalReading(user, type, Instant.EPOCH);
        r.setValue(new BigDecimal(value));
        r.setMeasuredAt(Instant.parse(at));
        return readings.save(r);
    }

    @Test
    void findsUsersByEmailIgnoringCase() {
        assertThat(users.findByEmailIgnoreCase("SAM@example.com")).isPresent();
        assertThat(users.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void readingsInRangeAreNewestFirstAndScopedToTheUser() {
        reading(userId, VitalType.WEIGHT, "82.0", "2026-01-01T07:00:00Z");
        reading(userId, VitalType.WEIGHT, "81.5", "2026-01-03T07:00:00Z");
        reading(userId, VitalType.HEART_RATE, "64", "2026-01-02T07:00:00Z");
        reading(userId, VitalType.WEIGHT, "80.0", "2026-02-01T07:00:00Z");
        reading(otherUserId, VitalType.WEIGHT, "60.0", "2026-01-02T07:00:00Z");

        List<VitalReading> weights = readings.findInRange(
                userId, VitalType.WEIGHT, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));
        List<VitalReading> all = readings.findInRange(
                userId, null, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

        assertThat(weights).extracting(r -> r.getValue().doubleValue()).containsExactly(81.5, 82.0);
        assertThat(all).hasSize(3);
        assertThat(readings.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(userId, VitalType.WEIGHT))
                .hasValueSatisfying(r -> assertThat(r.getValue()).isEqualByComparingTo("80.0"));
    }

    @Test
    void symptomNamesAreOrderedByFrequency() {
        for (String name : List.of("Headache", "Fatigue", "Headache", "Nausea", "Headache", "Fatigue")) {
            Symptom s = new Symptom(userId, Instant.EPOCH);
            s.setName(name);
            s.setSeverity(3);
            s.setOccurredAt(Instant.parse("2026-01-01T10:00:00Z"));
            symptoms.save(s);
        }

        assertThat(symptoms.findNamesByFrequency(userId)).containsExactly("Headache", "Fatigue", "Nausea");
        assertThat(symptoms.findNamesByFrequency(otherUserId)).isEmpty();
    }

    @Test
    void findsAnEarlierSymptomIgnoringCase() {
        Symptom s = new Symptom(userId, Instant.EPOCH);
        s.setName("Migraine");
        s.setSeverity(5);
        s.setOccurredAt(Instant.parse("2026-01-01T10:00:00Z"));
        symptoms.save(s);

        assertThat(symptoms.findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(userId, "MIGRAINE"))
                .hasValueSatisfying(found -> assertThat(found.getName()).isEqualTo("Migraine"));
        assertThat(symptoms.findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(otherUserId, "migraine")).isEmpty();
    }

    @Test
    void medicationTimesRoundTripInOrder() {
        Medication m = new Medication(userId, "Metformin", Instant.EPOCH);
        m.setStartDate(LocalDate.of(2026, 1, 1));
        m.setTimes(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)));
        Long id = medications.save(m).getId();
        em.flush();
        em.clear();

        Medication loaded = medications.findByIdAndUserId(id, userId).orElseThrow();
        assertThat(loaded.getTimes()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        assertThat(medications.findByIdAndUserId(id, otherUserId)).isEmpty();
    }

    @Test
    void moodEntriesInRangeAreNewestFirst() {
        for (String at : List.of("2026-05-01T08:00:00Z", "2026-05-03T08:00:00Z", "2026-06-01T08:00:00Z")) {
            MoodEntry e = new MoodEntry(userId, Instant.EPOCH);
            e.setMood(3);
            e.setRecordedAt(Instant.parse(at));
            moods.save(e);
        }

        List<MoodEntry> may = moods.findByUserIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
                userId, Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"));

        assertThat(may).extracting(MoodEntry::getRecordedAt)
                .containsExactly(Instant.parse("2026-05-03T08:00:00Z"), Instant.parse("2026-05-01T08:00:00Z"));
    }

    @Test
    void waterInRangeIsOldestFirstAndScopedToTheUser() {
        water.save(new WaterIntake(userId, 500, Instant.parse("2026-06-10T12:00:00Z"), Instant.EPOCH));
        water.save(new WaterIntake(userId, 250, Instant.parse("2026-06-10T07:00:00Z"), Instant.EPOCH));
        water.save(new WaterIntake(otherUserId, 330, Instant.parse("2026-06-10T08:00:00Z"), Instant.EPOCH));

        List<WaterIntake> day = water.findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
                userId, Instant.parse("2026-06-10T00:00:00Z"), Instant.parse("2026-06-11T00:00:00Z"));

        assertThat(day).extracting(WaterIntake::getAmountMl).containsExactly(250, 500);
    }

    private void note(Long user, String day, String title, String body) {
        JournalEntry j = new JournalEntry(user, Instant.EPOCH);
        j.setEntryDate(LocalDate.parse(day));
        j.setTitle(title);
        j.setBody(body);
        journal.save(j);
    }

    @Test
    void journalSearchMatchesTitleOrBodyIgnoringCase() {
        note(userId, "2026-07-01", "Appointment", "Ask Dr. Weber about the dizziness");
        note(userId, "2026-07-02", "Dizzy again", "Stood up too fast");
        note(userId, "2026-07-03", null, "Good day, long walk");
        note(otherUserId, "2026-07-02", null, "dizziness");
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        assertThat(journal.search(userId, from, to, "DIZZ")).extracting(JournalEntry::getEntryDate)
                .containsExactly(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1));
        assertThat(journal.search(userId, from, to, null)).hasSize(3);
        assertThat(journal.search(userId, from, LocalDate.of(2026, 7, 1), null)).hasSize(1);
    }

    @Test
    void symptomTagsAreOrderedByFrequency() {
        for (List<String> tags : List.of(List.of("stress", "coffee"), List.of("stress"), List.of("weather", "stress"))) {
            Symptom s = new Symptom(userId, Instant.EPOCH);
            s.setName("Headache");
            s.setSeverity(4);
            s.setOccurredAt(Instant.parse("2026-09-01T10:00:00Z"));
            s.setTags(tags);
            symptoms.save(s);
        }
        em.flush();
        em.clear();

        assertThat(symptoms.findTagsByFrequency(userId)).containsExactly("stress", "coffee", "weather");
        assertThat(symptoms.findTagsByFrequency(otherUserId)).isEmpty();
    }
}
