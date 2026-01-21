package com.healthtracker;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthtracker.medication.Medication;
import com.healthtracker.medication.MedicationRepository;
import com.healthtracker.symptom.Symptom;
import com.healthtracker.symptom.SymptomRepository;
import com.healthtracker.user.User;
import com.healthtracker.user.UserRepository;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
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
}
