# health-tracker

A personal health log for people who need to keep an eye on a few numbers: blood
pressure, weight, glucose, sleep, the medications they take and how they feel.

> **Not medical advice.** Health Tracker is a record-keeping tool. The default ranges
> are general guidance for adults and may not be right for you. Talk to your doctor
> about your own targets.

## Features

- **Vitals**: weight, blood pressure, heart rate, blood glucose and sleep, with trend
  charts and readings flagged when they are outside a reference range
- **Medications**: daily schedules, a dose log (taken / skipped / missed) and adherence
- **Symptoms**: severity 1-10 with notes, plotted over time
- **Activity**: workouts and walks with weekly active minutes
- **Goals**: target weight, daily steps, active minutes, workouts and sleep
- **Settings**: your own reference ranges and time zone
- **CSV export** of everything, e.g. to share with a doctor

## Stack

| Part     | Tech                                                                       |
| -------- | -------------------------------------------------------------------------- |
| API      | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Flyway   |
| Database | PostgreSQL 17                                                              |
| Web      | React 19, TypeScript, Vite, React Router, TanStack Query, Recharts         |
| Tests    | JUnit 5, Mockito, `@WebMvcTest`, H2; Vitest and Testing Library            |

## Running with Docker

```bash
docker compose up --build
```

Then open http://localhost:5176 and sign in with the demo account:

- **Email:** `demo@example.com`
- **Password:** `demo-password`

The demo user is created on first start with about a year of readings, medications,
doses, symptoms and workouts. Set `SEED_DEMO=false` to start with an empty database.

| Service | Host port | Notes                            |
| ------- | --------- | -------------------------------- |
| web     | 5176      | nginx; proxies `/api` to the API |
| api     | 4300      | Spring Boot                      |
| db      | 5435      | Postgres 17                      |

Ports can be changed with `WEB_PORT`, `API_PORT` and `DB_PORT`; see `.env.example`.
`docker compose down -v` removes the database volume.

## Local development

You need Java 21, Node 22 and Docker (for Postgres).

```bash
docker compose up -d db

cd api
./mvnw spring-boot:run        # http://localhost:8080

cd web
npm install
npm run dev                   # http://localhost:5173
```

## Tests

```bash
cd api && ./mvnw test
cd web && npm test
```

Neither suite needs a running database.

## Layout

```
api/   Spring Boot service, one package per feature (controller, service, repository, dto)
web/   React app (api client, auth, components, pages)
```
