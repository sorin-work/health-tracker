# Roadmap

Things I might add to health-tracker. The app already covers the daily loop
(log vitals, take medications, note symptoms and mood, print a report for the
doctor), so nothing here is promised or scheduled. These are ideas, written
down with enough detail that I (or anyone else) can pick one up later without
starting from zero. Items may change, move between groups or be dropped.

What the app does today is in the [README](README.md).

- **Next**: small or well understood, and mostly about trust in the data and
  the account. I would start with these.
- **Later**: worth doing, but bigger or waiting on a decision.
- **Maybe**: not sure yet that they are worth the extra complexity.

Sizes are rough: **S** is an evening or two, **M** a few evenings up to a
couple of weekends, **L** several weeks of spare time.

Each item is meant to fit in one pull request and to be covered by the test
suite. The API is layered per feature under `api/src/main/java/com/healthtracker/`
(controller, service, repository), and the rules that matter are plain classes
without Spring (`RangeFlagger`, `DoseSchedule`, `SupplyEstimator`,
`AdherenceCalculator`, `GoalProgressCalculator`, `TrendAggregator`). New logic
worth testing belongs in a class like those, with a plain JUnit test next to
the existing ones under `api/src/test/java/com/healthtracker/`. Services are
tested with Mockito (`*ServiceTest`), controllers with `@WebMvcTest`
(`*ControllerTest`), and queries in `RepositoryTest` against H2. On the web
side, Vitest and Testing Library tests sit next to the file they cover
(`web/src/lib/*.test.ts`, `web/src/components/*.test.tsx`,
`web/src/pages/*.test.tsx`), so formatting and date logic belongs in
`web/src/lib`, not inside a page. Schema changes are new Flyway files under
`api/src/main/resources/db/migration` (the next one is `V16__...`). Paths
below are relative to `api/src/main/java/com/healthtracker/` when they name a
Java class, and to `web/src/` when they name a web file.

When an item ships, its row in the Overview says `Shipped` in the Group
column, and its section moves under a `## Shipped` heading at the end, cut
down to one paragraph on what was built and anything left out. The same pull
request updates the README (features, API overview, limits) and adds an entry
under `## Unreleased` in `CHANGELOG.md`. There is no CHANGELOG yet: the first
item to ship creates it, with that heading.

"Depends on" lists real prerequisites only. Items that merely touch the same
file are not listed there; the bigger overlaps are mentioned in the items.

## Overview

| #   | Item                                              | Group | Area        | Size | Depends on |
| --- | ------------------------------------------------- | ----- | ----------- | ---- | ---------- |
| 1   | Change the password and delete the account        | Next  | account     | S    | -          |
| 2   | Schedule and time zone changes keep dose history  | Next  | medications | M    | -          |
| 3   | Slow down password guessing                       | Next  | security    | S    | -          |
| 4   | Tests against PostgreSQL                          | Next  | tests       | M    | -          |
| 5   | Full backup and restore of an account             | Next  | data        | M    | -          |
| 6   | Refuse unsafe production settings                 | Later | ops         | S    | -          |
| 7   | Security headers from nginx                       | Later | security    | S    | -          |
| 8   | Sessions that can be ended                        | Later | security    | M    | 1          |
| 9   | Record when a dose was really taken               | Later | medications | S    | -          |
| 10  | Split doses and tapering schedules                | Later | medications | M    | 2          |
| 11  | More vital types                                  | Later | vitals      | M    | -          |
| 12  | Blood pressure sessions with pulse                | Later | vitals      | M    | -          |
| 13  | Height, BMI and a smoothed weight trend           | Later | vitals      | S    | -          |
| 14  | Glucose time in range                             | Later | vitals      | M    | -          |
| 15  | Import every dataset, in the user's units         | Later | import      | M    | -          |
| 16  | Health profile on the report                      | Later | report      | M    | -          |
| 17  | Appointments and questions for the doctor         | Later | journal     | M    | -          |
| 18  | Charts in the doctor report                       | Later | report      | M    | -          |
| 19  | Reminders while the app is open                   | Later | medications | S    | -          |
| 20  | Read-only report link for a doctor                | Later | sharing     | M    | -          |
| 21  | Password reset by email                           | Later | account     | M    | 8          |
| 22  | Two-step sign-in                                  | Later | security    | M    | 1          |
| 23  | Accessibility pass                                | Later | ui          | M    | -          |
| 24  | Metrics and structured logs for operators         | Later | ops         | M    | -          |
| 25  | Scheduled database backups                        | Later | ops         | S    | -          |
| 26  | Undo for deletes                                  | Later | data        | M    | -          |
| 27  | Patterns between symptoms, sleep and mood         | Later | insights    | M    | -          |
| 28  | Lab results                                       | Maybe | vitals      | L    | -          |
| 29  | Reference ranges with a start date                | Maybe | vitals      | M    | -          |
| 30  | Faster on years of data                           | Maybe | performance | M    | -          |
| 31  | Push reminders when the app is closed             | Maybe | medications | L    | 19         |
| 32  | Installable app and offline logging               | Maybe | ui          | L    | -          |
| 33  | Apple Health and Health Connect imports           | Maybe | import      | L    | 15         |
| 34  | Caregiver access                                  | Maybe | sharing     | L    | 20         |
| 35  | Several people in one household                   | Maybe | account     | L    | 34         |
| 36  | Notes encrypted at rest                           | Maybe | security    | M    | -          |
| 37  | Translations and local date formats               | Maybe | ui          | M    | -          |

## Next

### 1. Change the password and delete the account

`/api/me` can change the display name, time zone and units, and that is all.
There is no way to change a password, and no way to leave: an account full of
health data stays in the database until someone runs SQL. For an app that
holds blood pressure, medications and a journal, both are basic.

- `PUT /api/me/password` with `currentPassword` and `newPassword`, in a new
  `ChangePasswordRequest` with the same `@Size(min = 8, max = 100)` as
  `RegisterRequest`. `UserService` checks the current one with the
  `PasswordEncoder` and answers 400 `invalid_credentials` on a mismatch (not
  401: the web client treats 401 as an expired session and signs the user
  out, see `send` in `api/client.ts`).
- `DELETE /api/me` with the password in the body. Every table already has
  `references users (id) on delete cascade` (V1 to V15), so deleting the row
  removes the rest. A `RepositoryTest` case should prove that for each table,
  so a future table without the cascade fails a test instead of blocking the
  delete.
- Settings gets an "Account" card: a password form, and a "Delete my account"
  button behind a `Modal` that asks for the password and names what goes
  (readings, medications, notes) and that it cannot be undone. It links to the
  export page first.
- Tricky: tokens issued before the change stay valid until they expire
  (`JWT_TTL` is seven days). This item does not fix that; item 8 does. Say so
  in the README limits until then.
- Tricky: the demo account. With `SEED_DEMO=true` the seeder recreates it on
  the next start, which is fine, but changing its password on a shared demo
  locks others out. I would refuse both actions for `DemoDataSeeder.DEMO_EMAIL`
  while seeding is on, with a clear message.
- Tests: `UserServiceTest` for wrong current password, success, and delete;
  `MeControllerTest` (new) for validation and the 400; a web test for the
  confirmation modal.

### 2. Schedule and time zone changes keep dose history

`DoseSchedule.expand` rebuilds every past slot from the medication's current
times and the user's current zone, and `DoseLog` rows are matched to slots by
exact instant (`key(medicationId, scheduledAt)`). Two ordinary actions break
the history:

- Moving a morning dose from 08:00 to 09:00 in `MedicationForm`. Every past
  day now has a 09:00 slot with no log (missed) and the logged 08:00 doses no
  longer match any slot, so they drop out of the day view and of adherence.
  A year at 95% turns into a year at 0%.
- Changing the time zone in Settings, for example after moving or on a long
  trip. All slots shift by the offset, with the same effect.

The fix is to stop deriving the past from the present.

- Data: a `medication_schedules` table (`medication_id`, `valid_from date`,
  `valid_to date null`, `schedule_type`, `interval_days`, `days_of_week`,
  `zone`) with its times in `medication_schedule_times`. The migration copies
  the current schedule of every medication into one row starting at its
  `start_date`, using the owner's time zone.
- `MedicationService.update` closes the open schedule at yesterday and opens a
  new one from today when the times, schedule type or days change. Name,
  dosage and instructions stay plain columns.
- `DoseSchedule.slotsOn` takes the schedule valid on that day instead of the
  medication, and the zone stored on the schedule instead of the user's. A
  time zone change in `UserService.updateProfile` opens new schedule rows from
  today for every active medication.
- Tricky: changing today's times. Doses already logged today keep their slot,
  so "from today" can show both the old logged slot and the new pending one.
  I would start the new schedule tomorrow when anything is logged today.
- Tricky: `isScheduledOn` for `EVERY_N_DAYS` counts from `start_date`. It
  should count from the schedule's `valid_from`, or a change of interval
  jumps the phase of the old one. Pick one and test it.
- Tricky: the dose log export and the report read through the same
  expansion, so they are fixed together; check both in tests.
- Undecided: whether a past schedule can be edited ("I actually took it at
  21:00 since March"). I would leave that out; it is rare and easy to get
  wrong.
- Tests: `DoseScheduleTest` cases for a time change mid-range, a zone change,
  a DST day and an every-N-days phase; `DoseServiceTest` for adherence across
  a change; the migration is covered by item 4 once it exists.

### 3. Slow down password guessing

`POST /api/auth/login` answers as fast as BCrypt allows, forever, for any
number of attempts. With a personal health log on the open internet that is
the obvious first attack.

- A small `LoginThrottle` in `security/` that counts failures per normalised
  email and per client address in a bounded in-memory map (for example
  Caffeine, or a `ConcurrentHashMap` with a sweep). After five failures for an
  email within 15 minutes, further attempts answer 429 `too_many_attempts`
  with `Retry-After`, without checking the password.
- The client address comes from `X-Forwarded-For` only when the request came
  through the bundled nginx; behind other proxies that header is whatever the
  client sends. A `app.trusted-proxies` setting (default: the compose network)
  decides.
- Tricky: user enumeration. `AuthService.login` skips the BCrypt check when
  the email is unknown (`findByEmailIgnoreCase` is empty), so unknown emails
  answer faster. Compare against a fixed dummy hash in that case, and throttle
  unknown emails the same way.
- Tricky: one process only. The compose stack runs one API container, which
  is the supported setup; say in the README that the counter is per process.
- Registration needs the same, per address, so nobody fills the database with
  accounts.
- The login page shows the 429 message with the wait time instead of
  "Email or password is incorrect".
- Tests: a plain test for `LoginThrottle` with a fixed `Clock`;
  `AuthControllerTest` for the 429 and the header.

### 4. Tests against PostgreSQL

All repository tests run on H2 with Flyway switched off and
`ddl-auto=create-drop` (`RepositoryTest`). So the migrations are never run in
the test suite, the `check` constraints in them are never tested, and queries
that only Postgres rejects pass. That already happened once: the journal list
failed on Postgres without a search term (the comment on
`JournalRepository.search` explains the null parameter inside `lower()`).

- Add Testcontainers (`org.testcontainers:postgresql`) as a test dependency
  and a `PostgresTest` base class with `@DataJpaTest`, Flyway on and
  `ddl-auto=validate`, against `postgres:17-alpine` like the compose file.
- Move the query tests from `RepositoryTest` there, or run both. The H2 copy
  can go once the Postgres one is trusted.
- One test that runs all migrations on an empty database and then lets
  Hibernate validate the mapping, so a missing column fails in CI and not on
  start-up.
- Tricky: CI time and Docker. GitHub's `ubuntu-latest` has Docker, so the
  `api` job keeps working; locally `./mvnw test` now needs Docker. I would tag
  the Postgres tests and keep `./mvnw test` without them, and run them in
  `./mvnw verify` (which CI already uses) through the failsafe plugin.
- Tests: this item is the test. It should include the cascade check from
  item 1 if that lands first.

### 5. Full backup and restore of an account

The export page writes eight CSV files. That is good for a spreadsheet, but
not a backup: custom ranges, goals, medication schedules, supply, the
settings and the link between doses and medications cannot be read back in.
The only import is vitals. Someone moving to another server, or recovering
from a mistake, has no way back.

- `GET /api/export/backup.json`: one JSON document with a `formatVersion`,
  the profile settings and every row of every table for the user, ids
  included so references (dose log to medication, tags to symptom) survive.
  A new `export/BackupService` builds it from the existing repositories.
- `POST /api/import/backup` restores it into an empty account only: refuse
  when the account has any data, so it never merges. Ids are remapped on
  insert; everything happens in one transaction.
- Settings or the export page gets "Download a full backup" and, on an empty
  account, "Restore from a backup", with the same preview and confirm step as
  the CSV import.
- Tricky: format changes. Each migration that adds a column must either keep
  the backup loader working or bump `formatVersion` with a converter. A test
  that round-trips the seeded demo data catches most of it.
- Tricky: size. A year of the demo data is a few thousand readings plus dose
  logs; ten years of minute-by-minute imports would not fit in memory as one
  tree. Stream the output with Jackson's generator; the upload limit is 5 MB
  today (`spring.servlet.multipart`), which the restore needs to raise for
  this endpoint only.
- Undecided: whether item 2's schedule history is in the format from the
  start. If item 2 ships first, yes; otherwise the converter handles it.
- Tests: `BackupServiceTest` round trip (export, restore into a new user,
  compare); controller tests for the non-empty account refusal.
