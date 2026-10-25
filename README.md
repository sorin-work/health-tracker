# health-tracker

A personal health log for people who need to keep an eye on a few numbers: blood
pressure, weight, glucose, sleep, the medications they take and how they feel. It
flags readings that fall outside a reference range, tracks medication adherence and
exports everything as CSV so it can be shared with a doctor.

> **Not medical advice.** Health Tracker is a record-keeping tool. The default ranges
> are general guidance for adults and may not be right for you. Talk to your doctor
> about your own targets, and contact emergency services if you feel unwell.

## Features

- **Vitals**: weight, blood pressure, resting heart rate, blood glucose (with meal
  context) and sleep. Trend charts per type with 7 day / 30 day / 90 day / 1 year or
  custom date ranges, min/max/average and a paged reading history.
- **Out-of-range flags**: every reading is classified as very low, low, in range,
  high or very high. Blood pressure uses the worse of systolic and diastolic, glucose
  uses a stricter range when fasting. Users can replace the default bounds with the
  targets their doctor gave them; the critical limits always stay.
- **Medications**: name, dose, instructions, times of day, start and end dates, or
  "as needed". Schedules can be every day, every N days (counted from the start date)
  or on chosen days of the week. Each dose is marked taken or skipped (and can be
  undone); doses with nothing logged count as missed once the day is over.
- **Supply and refills**: optionally track how many tablets (or puffs, ...) are left.
  Taking a dose counts the supply down, a refill adds a new pack, and the app works
  out how many days the supply lasts and warns on the dashboard before it runs out.
- **Adherence**: taken / (taken + skipped + missed) for any period, overall, per
  medication and per day.
- **Symptoms**: free-text name (with suggestions from earlier entries), severity
  1-10, duration and notes, plotted over time.
- **Activity**: walks, runs, cycling, swims, strength, yoga and more, with duration,
  distance, steps and calories. Weekly active-minute totals.
- **Mood**: a quick check-in (1-5) with an optional energy level and note, a daily
  chart and a 7-day strip on the dashboard.
- **Water**: one-tap glasses (250 / 500 ml), a daily total against a target and a
  per-day chart.
- **Journal**: dated notes with an optional title and full-text search, e.g. how a new
  medication is going or questions for the next appointment.
- **Goals**: target weight (with an optional target date and an on-track check),
  daily steps, weekly active minutes, weekly workouts, nightly sleep and daily water,
  with progress computed from the logged data.
- **CSV export** of vitals, the dose log (including missed doses), the medication
  list, symptoms, activities, mood, water and journal notes, in the user's time zone.
- **Units**: metric or imperial, and mg/dL or mmol/L for glucose, per user.
- **Dashboard** with the latest reading of each vital and its change on the week
  before, today's doses, 30-day adherence, recently flagged readings, what is coming
  up (doses still due, refills, goal dates), water, mood and goal progress.

## Stack

| Part     | Tech                                                                         |
| -------- | ---------------------------------------------------------------------------- |
| API      | Java 21, Spring Boot 3.5 (Web, Validation, Data JPA, Security), JWT (jjwt)   |
| Database | PostgreSQL 17, Flyway migrations                                             |
| Web      | React 19, TypeScript, Vite, React Router, TanStack Query, Recharts           |
| Tests    | JUnit 5, Mockito, AssertJ, `@WebMvcTest`, H2 for repository tests; Vitest and Testing Library |
| Ops      | Docker Compose, multi-stage Dockerfiles, nginx, GitHub Actions               |

## Architecture

```
browser ──> web (nginx :80) ──/api──> api (Spring Boot :8080) ──> db (Postgres :5432)
             static React app          JWT auth, Flyway on start
```

The browser only talks to the web container; nginx serves the built React app and
forwards `/api` to the API. In development, Vite's dev server does the same proxying.

The API is layered per feature (`vital`, `medication`, `symptom`, `activity`, `goal`,
`mood`, `water`, `journal`, `range`, `export`, `user`): a controller with request/response DTOs, a service with
the business rules and a Spring Data repository. The rules that matter most are plain
classes with no Spring dependencies, so they are easy to test:

| Class                    | What it does                                                    |
| ------------------------ | --------------------------------------------------------------- |
| `RangeFlagger`           | Classifies a reading against default or custom reference ranges |
| `DoseSchedule`           | Expands medication schedules into dose slots and their status   |
| `SupplyEstimator`        | Days of supply left at the current schedule, low-supply check   |
| `AdherenceCalculator`    | Taken / skipped / missed counts and adherence %                 |
| `GoalProgressCalculator` | Progress %, achieved and on-track for each goal type            |
| `TrendAggregator`        | Daily averages for charts, grouped in the user's time zone      |
| `CsvWriter`              | RFC 4180 CSV with protection against spreadsheet formulas       |

Authentication is stateless: `POST /api/auth/login` returns a signed JWT that the web
app keeps in `localStorage` and sends as `Authorization: Bearer …`. Every query is
scoped to the signed-in user.

## Running with Docker

```bash
cp .env.example .env    # optional, the defaults work
docker compose up --build
```

Then open http://localhost:5176 and sign in with the demo account:

- **Email:** `demo@example.com`
- **Password:** `demo-password`

On first start the API runs the Flyway migrations and, because `SEED_DEMO=true`,
creates the demo user with about a year of data: daily weight and blood pressure,
glucose and sleep, four medications with a realistic dose log (including some missed
and skipped doses), symptoms, workouts, mood check-ins, water, a few journal notes
and some goals. Set `SEED_DEMO=false` for an
empty database.

| Service | Host port | URL                                       | Notes                                |
| ------- | --------- | ----------------------------------------- | ------------------------------------ |
| web     | 5176      | http://localhost:5176                     | nginx; proxies `/api` to the API     |
| api     | 4300      | http://localhost:4300/actuator/health     | Spring Boot                          |
| db      | 5435      | `postgresql://health:health@localhost:5435/health` | Postgres 17                 |

Ports are configurable with `WEB_PORT`, `API_PORT` and `DB_PORT` (in `.env` or inline,
e.g. `WEB_PORT=8081 docker compose up`). Data lives in the `db-data` volume;
`docker compose down -v` removes it.

Quick check from the command line:

```bash
curl http://localhost:4300/actuator/health
curl -X POST http://localhost:4300/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"demo-password"}'
```

## Running without Docker

You need Java 21, Node 22 and a PostgreSQL database. The easiest way to get the
database is still Docker:

```bash
docker compose up -d db
```

API (http://localhost:8080):

```bash
cd api
SEED_DEMO=true ./mvnw spring-boot:run
```

It connects to `jdbc:postgresql://localhost:5435/health` as `health`/`health` by
default. Override with `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD`.

Web (http://localhost:5173):

```bash
cd web
npm install
npm run dev
```

Vite proxies `/api` to `http://localhost:8080`; set `API_URL` to point it elsewhere.

### Configuration

| Variable               | Default                                   | Used by |
| ---------------------- | ----------------------------------------- | ------- |
| `DATABASE_URL`         | `jdbc:postgresql://localhost:5435/health` | api     |
| `DATABASE_USER`        | `health`                                  | api     |
| `DATABASE_PASSWORD`    | `health`                                  | api     |
| `JWT_SECRET`           | a development value (min. 32 bytes)       | api     |
| `JWT_TTL`              | `7d`                                      | api     |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`                   | api     |
| `SEED_DEMO`            | `false` (`true` in docker compose)        | api     |
| `PORT`                 | `8080`                                    | api     |
| `API_URL`              | `http://localhost:8080`                   | web dev server |

## Tests

```bash
cd api && ./mvnw test      # JUnit + Mockito, @WebMvcTest, H2 repository tests
cd web && npm test         # Vitest + Testing Library
cd web && npm run typecheck
```

Neither suite needs a running database. CI (`.github/workflows/ci.yml`) runs both,
builds the web app and builds the Docker images.

## API overview

All endpoints except `/api/auth/*` and `/actuator/health` need a bearer token.
Dates are `YYYY-MM-DD` in the user's time zone; instants are ISO 8601 in UTC.

| Method & path                                  | Purpose                                      |
| ---------------------------------------------- | -------------------------------------------- |
| `POST /api/auth/register`, `POST /api/auth/login` | Create an account / sign in, returns a token |
| `GET/PUT /api/me`                              | Profile (name, time zone, units)             |
| `GET/POST /api/vitals`, `PUT/DELETE /api/vitals/{id}` | Readings; filter by `type`, `from`, `to`, `flagged` |
| `GET /api/vitals/summary`                      | Latest reading and 30-day stats per type     |
| `GET /api/vitals/trend?type=…`                 | Daily averages for charts                    |
| `GET /api/ranges`, `PUT/DELETE /api/ranges/{metric}` | Reference ranges, custom bounds, reset |
| `GET/POST /api/medications`, `PUT/DELETE /api/medications/{id}` | Medications            |
| `POST /api/medications/{id}/archive`           | Stop a medication, keeping its history       |
| `POST /api/medications/{id}/refill`            | Add a new pack to the tracked supply         |
| `GET /api/medications/low-supply`              | Active medications that are running out      |
| `POST /api/medications/{id}/doses`             | Log a dose as taken or skipped               |
| `GET /api/doses?date=…`, `DELETE /api/doses/{logId}` | A day's schedule / undo a logged dose  |
| `GET /api/doses/adherence?from=…&to=…`         | Adherence overall, per medication, per day   |
| `GET/POST /api/symptoms`, `GET /api/symptoms/names` | Symptom log and name suggestions        |
| `GET/POST /api/activities`, `GET /api/activities/weekly` | Activity log and weekly totals     |
| `GET/POST /api/mood`, `GET /api/mood/daily`    | Mood check-ins and daily averages            |
| `GET/POST /api/water`, `GET /api/water/daily`  | Today's water (or `?date=`), daily totals    |
| `GET/POST /api/journal?q=…`                    | Journal notes, searchable                    |
| `GET/POST /api/goals`, `POST /api/goals/{id}/archive` | Goals with live progress              |
| `GET /api/export/{dataset}.csv`                | `vitals`, `doses`, `medications`, `symptoms`, `activities`, `mood`, `water`, `journal` |

Errors share one shape:
`{ "status": 400, "error": "validation_failed", "message": "…", "fields": { "value": "…" } }`.

## Project layout

```
api/
  src/main/java/com/healthtracker/
    common/        errors, paging, date ranges, CSV writer
    config/        security, JWT properties, clock
    security/      JWT service and filter
    user/          registration, login, profile
    vital/ range/ medication/ symptom/ activity/ goal/ export/
    mood/ water/ journal/
    seed/          demo data
  src/main/resources/db/migration/   Flyway migrations
web/
  src/
    api/           typed API client, one module per resource
    auth/          auth context and route guards
    components/    shared UI, forms and charts
    lib/           date, formatting and range helpers
    pages/         one component per route
docker-compose.yml
```

## Assumptions and limits

- Everything is stored in metric units (kg, mg/dL, mmHg, km, ml). Settings switches the
  display and the forms to imperial (lb, mi, fl oz) and glucose to mmol/L; blood
  pressure, heart rate and sleep have no unit choice. Imports are always metric.
- Medication times are in the user's time zone, set in Settings. A schedule is one set
  of times; "two tablets in the morning, one at night" is two medications or one with
  a dose quantity of 1 and two times.
- One user per account; there is no sharing or caregiver access. Export the CSV
  files to share data.
