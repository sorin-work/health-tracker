# health-tracker

A personal health log: vitals (weight, blood pressure, heart rate, glucose, sleep),
medications with a daily dose log and adherence, and out-of-range flags for readings.

> Not medical advice. This is a record-keeping tool; talk to your doctor about your
> own targets.

## Stack

- **API**: Java 21, Spring Boot 3, Spring Security with JWT, Spring Data JPA, Flyway, PostgreSQL
- **Web**: React, TypeScript, Vite, React Router, TanStack Query, Recharts

## Local development

You need Java 21, Node 22 and Docker (for Postgres).

```bash
# Postgres on localhost:5435 (user/password/db: health)
docker compose up -d db

# API on http://localhost:8080, applies migrations on start
cd api
./mvnw spring-boot:run

# Web app on http://localhost:5173, proxies /api to the API
cd web
npm install
npm run dev
```

Open http://localhost:5173 and create an account.

## Tests

```bash
cd api && ./mvnw test
```

The API tests use mocks and don't need a database.

## Layout

```
api/   Spring Boot service, one package per feature (controller, service, repository, dto)
web/   React app (api client, auth, components, pages)
```
