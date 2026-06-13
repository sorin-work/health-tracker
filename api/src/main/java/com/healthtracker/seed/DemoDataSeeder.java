package com.healthtracker.seed;

import com.healthtracker.activity.ActivityType;
import com.healthtracker.goal.GoalType;
import com.healthtracker.medication.DoseSchedule;
import com.healthtracker.medication.Medication;
import com.healthtracker.medication.MedicationRepository;
import com.healthtracker.user.User;
import com.healthtracker.user.UserRepository;
import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates demo@example.com / demo-password with about a year of believable data: a slow weight
 * loss, blood pressure that improves after a medication is started, some missed doses, the odd
 * headache and a walking habit. Runs on startup when {@code app.seed-demo=true}; does nothing if
 * the demo user already exists.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "demo-password";
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final int DAYS = 365;

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final MedicationRepository medications;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(
            UserRepository users,
            MedicationRepository medications,
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.medications = medications;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Demo user already exists, skipping seed");
            return;
        }
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate first = today.minusDays(DAYS - 1);
        Random random = new Random(20251115L);

        User user = users.save(new User(
                DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Alex Demo", ZONE.getId(), first.atStartOfDay(ZONE).toInstant()));
        Long userId = user.getId();

        int readings = seedVitals(userId, first, today, now, random);
        int doses = seedMedications(userId, first, today, now, random);
        int symptoms = seedSymptoms(userId, first, today, now, random);
        int activities = seedActivities(userId, first, today, now, random);
        int moods = seedMood(userId, first, today, now, random);
        int glasses = seedWater(userId, first, today, now, random);
        seedGoals(userId, today, now);

        log.info("Seeded demo user {} with {} readings, {} dose logs, {} symptoms, {} activities, {} mood check-ins"
                + " and {} water entries", DEMO_EMAIL, readings, doses, symptoms, activities, moods, glasses);
    }

    private int seedVitals(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        List<Object[]> rows = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            double t = (double) (day.toEpochDay() - first.toEpochDay()) / DAYS; // 0 -> 1 over the year
            boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;

            // Weight: about 92 kg down to 84.5 kg, with a holiday bump around a third of the way in.
            if (random.nextDouble() < 0.8) {
                double holiday = Math.exp(-Math.pow((t - 0.12) / 0.03, 2)) * 1.6;
                double kg = 92.0 - 7.5 * t + holiday + random.nextGaussian() * 0.35;
                rows.add(reading(userId, VitalType.WEIGHT, kg, null, null, at(day, 7, 10, random), null, now));
            }

            // Blood pressure: elevated until lisinopril starts (~day 35), then settles.
            int bpReadings = random.nextDouble() < 0.75 ? (random.nextDouble() < 0.5 ? 2 : 1) : 0;
            for (int i = 0; i < bpReadings; i++) {
                double effect = t < 0.1 ? 0 : Math.min(1, (t - 0.1) / 0.15);
                double sys = 141 - 20 * effect + random.nextGaussian() * 6 + (weekend ? 2 : 0);
                double dia = 90 - 15 * effect + random.nextGaussian() * 4;
                if (random.nextDouble() < 0.02) {
                    sys += 30;
                    dia += 15;
                }
                int hour = i == 0 ? 8 : 20;
                String note = sys > 160 ? "Stressful day" : null;
                rows.add(reading(userId, VitalType.BLOOD_PRESSURE, Math.round(sys), (double) Math.round(dia), null,
                        at(day, hour, 15, random), note, now));
            }

            // Resting heart rate improves as the walking habit sticks.
            if (random.nextDouble() < 0.7) {
                double bpm = 74 - 8 * t + random.nextGaussian() * 3.5;
                rows.add(reading(userId, VitalType.HEART_RATE, Math.round(bpm), null, null, at(day, 7, 5, random), null, now));
            }

            // Glucose: fasting most mornings, and an after-meal check a few times a week.
            if (random.nextDouble() < 0.6) {
                double mg = 103 - 10 * t + random.nextGaussian() * 6;
                rows.add(reading(userId, VitalType.BLOOD_GLUCOSE, Math.round(mg), null, GlucoseContext.FASTING,
                        at(day, 6, 40, random), null, now));
            }
            if (random.nextDouble() < 0.35) {
                double mg = 130 - 10 * t + random.nextGaussian() * 18;
                String note = mg > 180 ? "Pasta dinner" : null;
                rows.add(reading(userId, VitalType.BLOOD_GLUCOSE, Math.round(mg), null, GlucoseContext.AFTER_MEAL,
                        at(day, 21, 0, random), note, now));
            }

            // Sleep, logged on waking.
            if (random.nextDouble() < 0.85) {
                double hours = 6.9 + 0.5 * t + (weekend ? 0.5 : 0) + random.nextGaussian() * 0.6;
                hours = Math.max(3.5, Math.min(10.5, hours));
                rows.add(reading(userId, VitalType.SLEEP, Math.round(hours * 4) / 4.0, null, null,
                        at(day, 6, 30, random), null, now));
            }
        }
        rows.removeIf(r -> ((OffsetDateTime) r[5]).toInstant().isAfter(now));
        jdbc.batchUpdate(
                "insert into vital_readings (user_id, type, value, secondary_value, glucose_context, measured_at, notes, created_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?, ?)",
                rows);
        return rows.size();
    }

    private int seedMedications(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        Medication metformin = medication(userId, "Metformin", "500 mg", "With breakfast and dinner",
                first, null, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), now);
        Medication lisinopril = medication(userId, "Lisinopril", "10 mg", "Morning, with water",
                first.plusDays(35), null, List.of(LocalTime.of(8, 0)), now);
        Medication atorvastatin = medication(userId, "Atorvastatin", "20 mg", "Before bed",
                today.minusDays(200), null, List.of(LocalTime.of(21, 30)), now);
        Medication vitaminD = medication(userId, "Vitamin D3", "1000 IU", null,
                first, today.minusDays(120), List.of(LocalTime.of(8, 0)), now);
        vitaminD.setActive(false);
        medications.save(vitaminD);
        Medication ibuprofen = medication(userId, "Ibuprofen", "200 mg", "As needed for headaches, max 3 a day",
                first, null, List.of(), now);

        List<Object[]> rows = new ArrayList<>();
        for (Medication m : List.of(metformin, lisinopril, atorvastatin, vitaminD)) {
            for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
                boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
                for (Instant slot : DoseSchedule.slotsOn(m, day, ZONE)) {
                    if (slot.isAfter(now)) {
                        continue;
                    }
                    boolean evening = slot.atZone(ZONE).getHour() >= 18;
                    double missChance = 0.05 + (weekend && evening ? 0.08 : 0);
                    double roll = random.nextDouble();
                    if (roll < missChance && day.isBefore(today)) {
                        continue; // nothing logged: shows up as missed
                    }
                    if (roll < missChance + 0.03) {
                        rows.add(dose(m, userId, slot, "SKIPPED", slot.plusSeconds(600), "Felt nauseous"));
                    } else {
                        Instant taken = slot.plusSeconds(random.nextInt(45 * 60));
                        rows.add(dose(m, userId, slot, "TAKEN", taken.isAfter(now) ? now : taken, null));
                    }
                }
            }
        }
        // A couple of ibuprofen doses a month.
        for (LocalDate day = first; !day.isAfter(today.minusDays(1)); day = day.plusDays(1)) {
            if (random.nextDouble() < 0.07) {
                Instant taken = at(day, 14, 0, random);
                rows.add(dose(ibuprofen, userId, taken, "TAKEN", taken, "Headache"));
            }
        }
        jdbc.batchUpdate(
                "insert into dose_logs (medication_id, user_id, scheduled_at, status, recorded_at, note)"
                        + " values (?, ?, ?, ?, ?, ?)",
                rows);
        return rows.size();
    }

    private int seedSymptoms(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        record Kind(String name, double chance, int minSeverity, int maxSeverity, String note) {}
        List<Kind> kinds = List.of(
                new Kind("Headache", 0.09, 2, 7, "Behind the eyes"),
                new Kind("Fatigue", 0.05, 3, 6, null),
                new Kind("Dizziness", 0.02, 2, 5, "When standing up quickly"),
                new Kind("Lower back pain", 0.03, 3, 8, "After gardening"),
                new Kind("Nausea", 0.015, 2, 5, "After morning metformin"));
        List<Object[]> rows = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today.minusDays(1)); day = day.plusDays(1)) {
            for (Kind kind : kinds) {
                if (random.nextDouble() < kind.chance()) {
                    int severity = kind.minSeverity() + random.nextInt(kind.maxSeverity() - kind.minSeverity() + 1);
                    String note = random.nextDouble() < 0.4 ? kind.note() : null;
                    rows.add(new Object[] {
                        userId, kind.name(), severity, odt(at(day, 9 + random.nextInt(11), 25, random)),
                        30 + random.nextInt(8) * 30, note, odt(now)
                    });
                }
            }
        }
        jdbc.batchUpdate(
                "insert into symptoms (user_id, name, severity, occurred_at, duration_minutes, notes, created_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?)",
                rows);
        return rows.size();
    }

    private int seedActivities(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        List<Object[]> rows = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            double t = (double) (day.toEpochDay() - first.toEpochDay()) / DAYS;
            DayOfWeek dow = day.getDayOfWeek();

            if (random.nextDouble() < 0.55 + 0.3 * t) {
                int minutes = 25 + random.nextInt(50);
                int steps = (int) (minutes * (105 + random.nextInt(20)));
                double km = steps * 0.00075;
                rows.add(activity(userId, ActivityType.WALK, at(day, 18, 30, random), minutes,
                        km, steps, minutes * 4, null, now));
            }
            if (t > 0.45 && (dow == DayOfWeek.SATURDAY) && random.nextDouble() < 0.8) {
                int minutes = 25 + random.nextInt(25);
                double km = minutes / (7.0 - random.nextDouble());
                rows.add(activity(userId, ActivityType.RUN, at(day, 9, 0, random), minutes,
                        km, (int) (km * 1350), minutes * 10, t > 0.9 ? "Parkrun" : null, now));
            }
            if ((dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) && random.nextDouble() < 0.6) {
                rows.add(activity(userId, ActivityType.STRENGTH, at(day, 19, 0, random), 40 + random.nextInt(20),
                        null, null, 220 + random.nextInt(80), null, now));
            }
            if (dow == DayOfWeek.SUNDAY && random.nextDouble() < 0.4) {
                rows.add(activity(userId, ActivityType.YOGA, at(day, 10, 0, random), 30 + random.nextInt(30),
                        null, null, 120, null, now));
            }
            if (dow == DayOfWeek.WEDNESDAY && t > 0.3 && t < 0.6 && random.nextDouble() < 0.5) {
                rows.add(activity(userId, ActivityType.SWIM, at(day, 7, 0, random), 30 + random.nextInt(15),
                        1.0 + random.nextDouble(), null, 300, null, now));
            }
        }
        rows.removeIf(r -> ((OffsetDateTime) r[2]).toInstant().isAfter(now));
        jdbc.batchUpdate(
                "insert into activities (user_id, type, started_at, duration_minutes, distance_km, steps, calories_kcal, notes, created_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                rows);
        return rows.size();
    }

    private int seedMood(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        List<Object[]> rows = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            if (random.nextDouble() > 0.8) {
                continue;
            }
            double t = (double) (day.toEpochDay() - first.toEpochDay()) / DAYS;
            boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
            // Slowly better over the year, a little better at weekends, and a dip in the dark weeks.
            double winter = Math.exp(-Math.pow((t - 0.2) / 0.06, 2)) * 0.7;
            double score = 3.1 + 0.8 * t + (weekend ? 0.3 : 0) - winter + random.nextGaussian() * 0.7;
            int mood = (int) Math.max(1, Math.min(5, Math.round(score)));
            Integer energy = random.nextDouble() < 0.75
                    ? (int) Math.max(1, Math.min(5, Math.round(score - 0.2 + random.nextGaussian() * 0.6)))
                    : null;
            String note = mood <= 2 && random.nextDouble() < 0.5 ? "Slept badly" : null;
            rows.add(new Object[] {userId, mood, energy, odt(at(day, 20, 60, random)), note, odt(now)});
        }
        rows.removeIf(r -> ((OffsetDateTime) r[3]).toInstant().isAfter(now));
        jdbc.batchUpdate(
                "insert into mood_entries (user_id, mood, energy, recorded_at, note, created_at) values (?, ?, ?, ?, ?, ?)",
                rows);
        return rows.size();
    }

    private int seedWater(Long userId, LocalDate first, LocalDate today, Instant now, Random random) {
        // Water tracking started a few months in; most days get a handful of glasses.
        int[] sizes = {200, 250, 250, 330, 500};
        List<Object[]> rows = new ArrayList<>();
        for (LocalDate day = today.minusDays(150); !day.isAfter(today); day = day.plusDays(1)) {
            if (day.isBefore(first) || random.nextDouble() < 0.15) {
                continue;
            }
            int glasses = 4 + random.nextInt(5);
            for (int i = 0; i < glasses; i++) {
                int hour = 7 + i * 14 / glasses;
                rows.add(new Object[] {userId, sizes[random.nextInt(sizes.length)], odt(at(day, hour, 40, random)), odt(now)});
            }
        }
        rows.removeIf(r -> ((OffsetDateTime) r[2]).toInstant().isAfter(now));
        jdbc.batchUpdate("insert into water_intake (user_id, amount_ml, logged_at, created_at) values (?, ?, ?, ?)", rows);
        return rows.size();
    }

    private void seedGoals(Long userId, LocalDate today, Instant now) {
        String sql = "insert into goals (user_id, type, target_value, start_value, start_date, target_date, notes, created_at)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, userId, GoalType.WEIGHT.name(), new BigDecimal("82.00"), new BigDecimal("89.50"),
                today.minusDays(150), today.plusDays(60), "Doctor suggested 82 kg by spring", odt(now));
        jdbc.update(sql, userId, GoalType.DAILY_STEPS.name(), new BigDecimal("8000"), null,
                today.minusDays(120), null, null, odt(now));
        jdbc.update(sql, userId, GoalType.WEEKLY_ACTIVE_MINUTES.name(), new BigDecimal("150"), null,
                today.minusDays(120), null, "WHO recommendation", odt(now));
        jdbc.update(sql, userId, GoalType.SLEEP_HOURS.name(), new BigDecimal("7.5"), null,
                today.minusDays(60), null, null, odt(now));
        jdbc.update(sql, userId, GoalType.DAILY_WATER.name(), new BigDecimal("2000"), null,
                today.minusDays(150), null, null, odt(now));
    }

    private Medication medication(Long userId, String name, String dosage, String instructions,
            LocalDate start, LocalDate end, List<LocalTime> times, Instant now) {
        Medication m = new Medication(userId, name, now);
        m.setDosage(dosage);
        m.setInstructions(instructions);
        m.setAsNeeded(times.isEmpty());
        m.setTimes(times);
        m.setStartDate(start);
        m.setEndDate(end);
        return medications.save(m);
    }

    private static Object[] reading(Long userId, VitalType type, double value, Double secondary,
            GlucoseContext context, Instant at, String notes, Instant now) {
        return new Object[] {
            userId, type.name(), dec(value), secondary == null ? null : dec(secondary),
            context == null ? null : context.name(), odt(at), notes, odt(now)
        };
    }

    private static Object[] dose(Medication m, Long userId, Instant slot, String status, Instant recorded, String note) {
        return new Object[] {m.getId(), userId, odt(slot), status, odt(recorded), note};
    }

    private static Object[] activity(Long userId, ActivityType type, Instant at, int minutes, Double km,
            Integer steps, Integer kcal, String notes, Instant now) {
        return new Object[] {
            userId, type.name(), odt(at), minutes, km == null ? null : dec(km), steps, kcal, notes, odt(now)
        };
    }

    /** A time on the given day, jittered by up to {@code jitterMinutes} either way. */
    private static Instant at(LocalDate day, int hour, int jitterMinutes, Random random) {
        int offset = jitterMinutes == 0 ? 0 : random.nextInt(jitterMinutes * 2 + 1) - jitterMinutes;
        return day.atTime(hour, 0).plusMinutes(offset + 30L).atZone(ZONE).toInstant();
    }

    private static BigDecimal dec(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static OffsetDateTime odt(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
