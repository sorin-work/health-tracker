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

## Later

### 6. Refuse unsafe production settings

The defaults are tuned for a laptop: `JWT_SECRET` falls back to
`local-development-secret-change-me-0123456789` in both `application.yml` and
`docker-compose.yml`, compose turns `SEED_DEMO` on, and the demo password is
in the README. Someone who puts the compose stack on a server as is gets a
forgeable token and a public demo login.

- A `StartupChecks` bean that, when `app.environment=production` (a new
  setting, default `development`), refuses to start if the secret is the
  development value or shorter than 32 bytes, or if `SEED_DEMO` is on, or if
  `CORS_ALLOWED_ORIGINS` still points at localhost.
- In development it logs one warning line instead.
- `.env.example` gets a production block with `APP_ENVIRONMENT=production`
  and a note on generating a secret (`openssl rand -base64 48`).
- Tests: plain tests for each refusal with a constructed properties object.

### 7. Security headers from nginx

`web/nginx.conf` sends no security headers. The token lives in
`localStorage`, so any script injection reads it, and a Content Security
Policy is the cheapest guard.

- Add `Content-Security-Policy` (`default-src 'self'`, `img-src 'self' data:`,
  `style-src 'self' 'unsafe-inline'` if Recharts needs it, `frame-ancestors
  'none'`), `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`
  and a `Permissions-Policy` that turns off camera, microphone and location.
- `Strict-Transport-Security` only when TLS is terminated in front; document
  it rather than send it from the plain-HTTP container.
- Tricky: `add_header` in a `location` block drops the server-level ones
  (that is how the `/assets/` block works today). Use an include file per
  location.
- Tricky: the print view and the CSV downloads must still work; check both by
  hand with the policy on.
- Tests: none in the suites; a `curl -I` check in the README and in the PR.

### 8. Sessions that can be ended

A token is valid for seven days and cannot be revoked. Changing the password
(item 1) or losing a phone leaves every issued token working.

- A `token_version integer` column on `users`, put into the JWT as a claim.
  `JwtAuthenticationFilter` compares it with the stored value; a password
  change, a delete and a new "Sign out everywhere" button bump it.
- That adds one user lookup per request. Cache it for a short time per user id
  or accept the cost; the app is small. I would accept it and measure.
- Shorter tokens with a refresh step would be better still, but need a second
  token and somewhere safe to keep it. With an `HttpOnly` cookie for the
  refresh token the access token could stay in memory instead of
  `localStorage`, which closes the XSS read as well. That is a bigger change
  to `AuthContext` and `api/client.ts`; I would do the token version first
  and leave cookies as an open question.
- Tests: `JwtServiceTest` for the claim; a filter test where a bumped version
  gives 401.

### 9. Record when a dose was really taken

`DoseLog` has `scheduled_at` and `recorded_at`, and `recorded_at` is when the
button was pressed. Marking yesterday's evening dose as taken this morning
records "taken" with no way to tell that it was taken late, or that it was
logged late but taken on time.

- An optional `taken_at timestamptz` on `dose_logs`. The dose list offers
  "Taken now" (default) and "Taken at ..." with a time picker.
- `AdherenceCalculator` gains a "late" count: taken more than a set window
  after the slot (an hour by default, per medication later if needed). It does
  not change the adherence percentage; it adds information.
- The doctor report shows late doses per medication, which matters for drugs
  where timing matters (thyroid, some anticoagulants).
- Tricky: as-needed doses have no slot; their `taken_at` is just the time.
- Tests: `AdherenceCalculatorTest` for the late count; `DoseServiceTest` for
  a `taken_at` in the future (refused) and before the slot day (refused).

### 10. Split doses and tapering schedules

The README admits the limit: "two tablets in the morning, one at night" is two
medications, because `dose_quantity` is one number for all times. A tapering
course (40 mg for a week, then 30, then 20) needs a new medication per step.

- Per-time quantity: `medication_schedule_times` from item 2 gets a
  `quantity` column, and `SupplyEstimator` sums the day's quantities instead
  of multiplying.
- Tapering: a sequence of schedule rows with set `valid_from` dates, created
  from a small "change the dose on ..." form. Item 2's table already holds it.
- `MedicationForm` grows a per-time quantity field; keep the simple form as
  the default.
- Tricky: the dashboard and report show "Dosage" as free text (`dosage`
  varchar). With steps, the current step's text should come from the schedule
  row, and the report should list the steps in the period.
- Tests: `SupplyEstimatorTest` with mixed quantities; `DoseScheduleTest` for a
  taper across three steps.

### 11. More vital types

`VitalType` has weight, blood pressure, heart rate, glucose and sleep. People
who track their health at home commonly ask for body temperature, oxygen
saturation (SpO2) and waist size, and a few for body fat and respiratory rate.

- New enum values with a unit, a `VitalValidator` range and a
  `RangeFlagger` default range for each (SpO2 low below 95, critical below 90;
  temperature high above 37.5 °C, critical above 39.5). The `type` column is a
  varchar, so no migration is needed for the type itself.
- Units: temperature in °C stored, °F shown with imperial; waist in cm and
  inches. `lib/units.ts` and `UnitPrefs` grow the conversions.
- `VitalsPage`, `TrendChart`, the summary, the CSV export and import, and the
  report all switch on the type. A single list of "vital definitions" in
  `lib/` (label, unit, decimals, chart colour) would stop that from spreading
  further, and is worth doing first.
- Tricky: the dashboard shows one tile per type. Eight tiles is a lot; let the
  user hide types they do not track (a `hidden_vitals` setting).
- Tests: validator and flagger cases per type; `units.test.ts` for the
  conversions; one `VitalsPage` test for a new type.

### 12. Blood pressure sessions with pulse

Home blood pressure guidance usually asks for two or three readings a minute
apart, morning and evening, and for the average to be used. Monitors also give
a pulse with every reading. Today each reading is separate, the pulse has to
be entered again as a heart rate reading, and the report averages single
readings.

- An optional `pulse` on blood pressure readings (a third value; the table
  has `value` and `secondary_value`, so a `tertiary_value` column or a
  dedicated `pulse` column, which I prefer for clarity).
- A "session" form that takes two or three readings and stores them with a
  shared `session_id`. Charts and the summary use the session average; the
  history list shows the readings under it.
- An optional arm (left, right) and position (sitting, standing) per reading.
- Tricky: flags. Flag the session average, not each reading, or a single
  nervous first reading shows as high every morning. Keep the per-reading
  flag visible in the history.
- Tricky: imports. The CSV columns get `pulse` and `session`, both optional.
- Tests: `TrendAggregatorTest` with sessions; `RangeFlaggerTest` for the
  average; form test for two and three readings.

### 13. Height, BMI and a smoothed weight trend

Weight is tracked daily, but the profile has no height, so there is no BMI,
and the weight chart shows the daily noise (water, salt) that hides the trend.

- `height_cm` on `users`, set in Settings, shown in imperial when chosen.
- BMI next to the latest weight on the dashboard and in the report, computed
  in a plain `BodyMass` class. Show it as a number with the standard bands and
  a note that it is a rough measure.
- A 7-day moving average line on the weight chart, computed in
  `lib/charts.ts` from the daily points `TrendAggregator` already returns.
- Tricky: gaps. A moving average over missing days should use the readings in
  the window, not treat a gap as zero.
- Tests: plain tests for the BMI and the moving average with gaps.

### 14. Glucose time in range

For people with diabetes the most used summary is time in range: the share of
readings between 70 and 180 mg/dL, with time below and above shown separately.
The app flags single readings but has no such summary, and the dashboard's
30-day average hides lows.

- A `GlucoseStats` class: share below, in and above the range, the number of
  readings, and the mean. Use the user's custom non-fasting range when set.
- A card on the vitals page for glucose, and a line in the report.
- Tricky: "time" in range assumes a continuous monitor. With finger-stick
  readings it is a share of readings, and the label must say so. Do not show an
  estimated HbA1c (GMI) from finger-sticks; it needs dense data.
- Tests: plain tests for the shares, an empty period and the custom range.

### 15. Import every dataset, in the user's units

The CSV import (`VitalImportService`) reads vitals only, and always in metric.
Symptoms, activities, mood, water and the journal can be exported but not
imported, and a user with imperial units who exports, edits and imports back
gets pounds stored as kilograms if they convert by hand.

- One importer per dataset with the same columns as `ExportService` writes,
  sharing the parsing, preview and duplicate check that `VitalImportService`
  already has (`withoutDuplicates`, `MAX_ROWS`, `unprotect`). A common
  `CsvImporter<T>` base keeps the row errors and preview the same.
- A `units` choice on the import page (default: the user's settings) that
  converts on the way in; the vitals export writes metric today, so default
  to metric when the header matches the export exactly.
- The dose log is the exception: it depends on medications and schedules, so
  it belongs to item 5's restore, not here.
- Tricky: duplicates for free-text rows (journal, symptoms). Match on time
  plus name or title; never silently drop a row that differs only in notes.
- Tests: importer tests per dataset; `ImportPage.test.tsx` for the dataset
  choice.

### 16. Health profile on the report

The doctor report covers vitals, medications, symptoms and notes, but the
first questions at a new appointment are about conditions, allergies and
contacts. There is nowhere to keep them.

- A `health_profile` table (one row per user): conditions, allergies (with the
  reaction), date of birth, blood type, emergency contact and the usual
  doctor, all optional free text with sensible lengths.
- A "Profile" section in Settings and a block at the top of the report.
- Tricky: date of birth is personal; keep it optional and do not use it for
  anything else unless a later item needs age-based ranges.
- An "emergency card" print view (one small page with the profile and current
  medications) is cheap once the data exists.
- Tests: service and controller tests; `ReportPage.test.tsx` for the block.

### 17. Appointments and questions for the doctor

The journal is used for "questions for the next appointment" (the README says
so), but those questions are scattered across dated notes and the report
cannot pick them up.

- An `appointments` table: date and time, who (free text), where, and notes.
  A list on a new page and the next one on the dashboard's "coming up" card
  (`lib/reminders.ts` gets a fourth kind).
- Questions: journal notes get an optional "ask at next appointment" flag;
  open questions show on the appointment and in the report. After the visit,
  each question can be marked answered with a short note.
- "Prepare report" on an appointment opens the report for the period since the
  previous appointment.
- Tests: `reminders.test.ts` for the new kind; service tests for the period.

### 18. Charts in the doctor report

The report is tables of averages and ranges. A doctor reads a trend faster
from a picture, and the trend charts already exist on the vitals page.

- Small print-friendly charts (weight, blood pressure, glucose) in the report,
  reusing `TrendChart` with a `compact` mode: no tooltips, no animation, fixed
  size, black and white friendly (patterns or line styles as well as colour).
- The data comes from the report endpoint so the page still makes one request;
  `ReportService` adds the daily trend points it already computes for the
  averages.
- Tricky: printing SVG charts across browsers. Test in Chrome and Firefox
  print preview, with `print-color-adjust`, and keep one page as the target.
- Tests: `ReportPage.test.tsx` checks the charts render with an accessible
  name; printing itself is checked by hand.

### 19. Reminders while the app is open

The dashboard lists overdue doses, but nothing tells a user who has the app
open in another tab that a dose is due.

- With the user's permission, the web app uses the Notification API to show a
  notice when a pending dose's time arrives, while a tab is open. A timer in a
  hook reads today's doses from the query cache; no server work.
- A setting to turn it on, and a test notification button.
- Tricky: tabs asleep in the background fire timers late; check on
  `visibilitychange` as well, and never notify twice for the same slot (keep
  the notified keys for the day in `sessionStorage`).
- Honest limit: this does not work when the browser is closed. Item 31 is the
  real fix, and much bigger.
- Tests: a hook test with fake timers and a mocked `Notification`.

### 20. Read-only report link for a doctor

The only way to share is printing or CSV files. A link to a read-only report,
valid for a few days, is what many people would send before a remote
appointment.

- `report_shares`: a random token (store its hash), the period, an expiry
  (at most 30 days), and a revoked flag. `POST /api/report/shares` creates one,
  the list shows active ones with a revoke button.
- `GET /api/public/report/{token}` returns the same `DoctorReport` without
  auth; a public route in the web app renders it with the print layout and no
  navigation.
- Tricky: this is the first unauthenticated data endpoint. It needs the
  security config to permit only that path, a rate limit (item 3's throttle),
  `Referrer-Policy: no-referrer` so the token does not leak in referers, and
  no indexing (`X-Robots-Tag: noindex`).
- Tricky: the report changes as data is edited. The link shows live data for
  the period; say so on the share form.
- Tests: controller tests for expired, revoked and unknown tokens; security
  test that nothing else became public.

### 21. Password reset by email

A user who forgets their password loses access to their history, and only a
database admin can help. The app has no email at all today.

- Optional SMTP settings (`MAIL_HOST`, ...). Without them, the reset page
  says to ask the administrator, and nothing else changes.
- `POST /api/auth/password-reset` always answers 202 (no enumeration) and,
  when the account exists, mails a link with a single-use token valid for an
  hour (hash stored). `POST /api/auth/password-reset/confirm` sets the new
  password and bumps the token version from item 8.
- A mailpit container in docker-compose for development, like the sibling
  repos, on a free port.
- Tests: service tests with a fake mail sender; expired and reused tokens.

### 22. Two-step sign-in

Health data is sensitive enough that some users want a second factor.

- TOTP (authenticator apps), no SMS: a secret per user, enrolment with a QR
  code, a confirmation code, and ten one-time recovery codes (hashed).
- Login becomes two steps when enabled: the first answers with a short-lived
  "half" token that only `POST /api/auth/totp` accepts.
- Disabling it, and changing the password, ask for a current code.
- Tricky: clock drift (allow one step either side) and code reuse (remember
  the last used step per user).
- Tests: a TOTP class with known test vectors from RFC 6238; service tests for
  the flow.

### 23. Accessibility pass

The README lists a skip link, visible focus, labelled charts and reduced
motion. Charts have an `aria-label`, but a label is a summary; a screen reader
user cannot read the values behind a trend.

- A "Show as table" toggle under every chart (`components/charts/*`), with the
  same points the chart draws.
- Check the dose list and forms with a screen reader: status changes
  (taken, skipped, undo) should be announced through a live region.
- Colour is the only signal in a few places (flag badges carry text, but
  chart lines do not); add line styles or markers.
- Run axe in the existing Testing Library tests (`vitest-axe`) for the main
  pages so regressions show in CI.
- Tests: the axe checks, plus tests for the table toggle.

### 24. Metrics and structured logs for operators

Actuator exposes only `health`. There are three log lines in the whole API.
Someone running this for a family has no way to see errors, slow requests or
disk growth without reading container logs.

- Expose `/actuator/prometheus` (Micrometer) on a separate management port
  that compose does not publish, so it is not reachable from the browser.
- JSON logs (Spring Boot 3.4+ has structured logging built in) with a request
  id that nginx passes in `X-Request-Id`, and log every 5xx with it.
- Never log request bodies: they carry health data.
- A short "Operating" section in the README: what to watch, how to read the
  logs, where the data lives.
- Tests: a context test that the endpoint is on the management port only.

### 25. Scheduled database backups

`docker compose down -v` or a broken disk loses everything. Item 5 is a
per-user backup; this one is for whoever runs the server.

- A `backup` service in docker-compose (the Postgres image with a small
  script) that runs `pg_dump` daily into a mounted folder and keeps the last
  14 dumps.
- A documented restore: stop the API, `psql` the dump into a fresh volume,
  start again. Flyway will see the schema history table and do nothing.
- Tricky: the dumps hold everyone's health data in plain text; say so, and
  suggest an encrypted destination.
- Tests: none in the suites; the PR shows a dump and a restore by hand.

### 26. Undo for deletes

Deleting a reading, symptom or note is immediate and final (`delete` in each
service). Deleting a medication also deletes its whole dose history through
the cascade, which can wipe a year of adherence with one click.

- For medications: deleting one with dose logs should be refused with a hint
  to archive it instead (archive already exists and keeps history). Delete
  stays for medications created by mistake with no logs.
- For everything else: a toast with "Undo" for a few seconds, which re-creates
  the item from the response the delete returned. Simpler than soft delete
  columns on every table, and enough for mistakes.
- Tricky: undo of a reading must keep the original `measured_at` and notes;
  undo of a symptom its tags.
- Tests: `MedicationServiceTest` for the refusal; a component test for the
  toast.

### 27. Patterns between symptoms, sleep and mood

Tags on symptoms exist "to look for triggers" (README), but the app leaves the
looking to the user. A simple, honest summary would help: for each tag, how
often and how severe symptoms are on days with it against days without, and
the same for short sleep and low mood.

- A plain `PatternFinder` that works on daily values: symptom count and
  maximum severity, sleep hours, mood average, tags present.
- A "Patterns" card on the symptoms page listing the few strongest
  differences, each with the number of days behind it.
- Tricky: this invites false conclusions. Show counts, require a minimum
  number of days (say 10 on each side), avoid the word "cause", and keep the
  "not medical advice" note visible.
- Tests: plain tests with constructed days, including too few days.

## Maybe

### 28. Lab results

Blood tests (HbA1c, cholesterol, kidney values, vitamin D) are the other
numbers people bring to appointments. They differ from vitals: each test has
its own unit, the lab prints its own reference range, and they come a few at
a time from one sample.

- `lab_results`: test name (with suggestions from earlier entries, like
  symptoms), value, unit, the lab's low and high, sample date, and an optional
  report group id.
- Flags come from the lab's range on the row, not from `RangeFlagger`.
- A chart per test and a table in the report.
- Tricky: units. The same test is reported in different units by different
  labs (mmol/mol and % for HbA1c). Store as entered; convert only for a short
  list of known tests, or not at all at first.
- Why maybe: it is a second, parallel vitals system. Worth it only if people
  actually log labs.

### 29. Reference ranges with a start date

Flags are computed when read, from the current ranges (`rangeService.flaggerFor`
in `VitalService`). When a doctor sets a new target, every old reading is
re-flagged against it, so last year's report changes after the fact.

- Store ranges with a `valid_from` date and flag each reading with the range
  valid at its time.
- Why maybe: re-flagging the past with today's target is arguably what a user
  wants on a chart. The report is where it matters most; the report could
  instead state which ranges it used. I would start with that note.

### 30. Faster on years of data

The app is fine with the seeded year. A few spots grow with history:

- The `flagged` filter on `GET /api/vitals` loads every reading in the range
  and pages in memory, because flags depend on ranges. A stored flag column,
  refreshed when ranges change, would let the database filter.
- `VitalService.summary` makes two queries per vital type; with more types
  (item 11) that grows. One grouped query would do.
- `DoseSchedule.expand` walks every day of every medication for adherence up
  to `MAX_RANGE_DAYS`. Fine for a year, not for a decade at once.
- Why maybe: none of this is slow today. Measure with ten years of generated
  data first (a `SEED_DEMO_YEARS` setting would help) and fix what is slow.

### 31. Push reminders when the app is closed

The reminder people actually want is a phone notification at dose time with
the app closed. That needs Web Push.

- A service worker, VAPID keys generated on first start (no hosted service;
  pushes go through the browser vendor's push service, which is how Web Push
  works), a `push_subscriptions` table, and a scheduler in the API that sends
  due reminders every minute.
- "Taken" and "Snooze" actions on the notification that call the API.
- Why maybe: it is the biggest piece of new infrastructure here (a scheduler,
  a worker, key handling), iOS only supports it for installed web apps, and a
  missed push for a medication is worse than no promise at all. It needs item
  32's installable app on iOS.

### 32. Installable app and offline logging

Most logging happens on a phone, often with a poor connection.

- A web manifest and a service worker that caches the app shell, so the app
  installs to the home screen and opens offline.
- Offline writes: queue new readings, doses and mood check-ins in IndexedDB
  and send them when back online, with the original times.
- Why maybe: the queue needs conflict handling (a dose logged offline on two
  devices) and idempotent creates (a client-generated id per entry). The
  installable shell alone is S and could be split out.

### 33. Apple Health and Health Connect imports

Many users already have years of weight, steps and sleep in Apple Health or
Android's Health Connect.

- Apple Health exports a zip with `export.xml`. A streaming XML reader can
  pick out weight, blood pressure, heart rate, glucose, sleep and workouts and
  feed them to item 15's importers.
- Health Connect has no file export of its own; third-party apps write CSV,
  which item 15 covers if the columns can be mapped.
- Why maybe: `export.xml` is often hundreds of MB, the upload limit is 5 MB,
  and sleep records need merging (many short samples per night). It needs a
  background job and a progress page, which the app does not have.

### 34. Caregiver access

The README says it plainly: one user per account, no sharing or caregiver
access. Adult children who help a parent with medications ask for this.

- An invitation by email (item 21's mail) that links a second account with
  read-only or "can log doses" access to the first.
- Every query is scoped to the signed-in user today (`AuthUser.id()` in every
  controller). Acting for someone else means a "current subject" separate
  from the signed-in user, checked in every service. That is the whole risk
  of this item: one missed check leaks health data.
- Why maybe: high value for some users, but it touches every endpoint. Item
  20's report link covers the doctor case with far less risk.

### 35. Several people in one household

One login that keeps logs for a child or an elderly parent who does not use
the app themselves.

- Profiles under one account, each with its own data and time zone; a switcher
  in the layout.
- Why maybe: it is item 34's "current subject" problem without the invitation
  part. If item 34 is built, this is mostly UI; if not, it is the same risk.

### 36. Notes encrypted at rest

Journal bodies, symptom notes and medication instructions are the most
personal text in the database, and a database dump shows them in plain text.

- Encrypt those columns in the API with a key from the environment (AES-GCM
  through a JPA `AttributeConverter`), so a stolen dump or backup is not
  readable without the key.
- Tricky: the journal search uses `like` in the database
  (`JournalRepository.search`). Encrypted columns cannot be searched there;
  search would move into the API over decrypted rows for the user, which is
  fine at one person's size.
- Tricky: key rotation and a lost key. A lost key loses the notes for good.
- Why maybe: it protects against dumps and backups, not against a compromised
  API. Disk encryption on the server covers much of the same ground with no
  code.

### 37. Translations and local date formats

All text is English and dates use one format. Health apps are often used by
older people in their own language.

- Extract strings in the web app into message files (for example with
  `react-intl` or a small typed dictionary) and format dates and numbers with
  `Intl` from a locale setting.
- The API error messages (`ApiException` messages) are shown to users as is;
  they would need codes the web app translates, which the `error` field
  partly provides already.
- Why maybe: a lot of churn across every page for a single maintainer, with no
  translator yet. Local date and number formats alone are S and could go
  first.
