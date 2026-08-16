# SafeEdge

Sports betting analysis and backtesting application.

SafeEdge ingests odds and event data, evaluates betting candidates, and backtests strategies. It does not place bets.

## Current phase

Technical scaffold. Tippmix API discovery. No betting/business functionality yet, and no automated betting.

## Stack

- Backend: Java 21, Spring Boot, Gradle, PostgreSQL, Liquibase, Spring Web, Validation, Data JPA, Actuator
- Frontend: Angular (standalone)
- Tests: JUnit 5, Testcontainers PostgreSQL

## Local PostgreSQL

Local development uses Docker Compose for PostgreSQL only. The backend and frontend still run on the host.

SafeEdge publishes PostgreSQL on host port **5433** so it does not collide with another local Postgres on 5432.

### Start PostgreSQL

```powershell
docker compose up -d
docker compose ps
```

Wait until `postgres` is `healthy` before starting the backend.

Expected host DB endpoint:

```text
localhost:5433
```

### Stop database

```powershell
docker compose down
```

This keeps the named volume `safeedge-postgres-data`, so local data survives restarts.

### Stop and delete local DB data

```powershell
docker compose down -v
```

This deletes the named PostgreSQL volume and **all local SafeEdge database data**.

## Local backend

`bootRun` activates the `local` profile automatically and uses these Compose defaults:

```text
DB_HOST=localhost
DB_PORT=5433
DB_NAME=safeedge
DB_USERNAME=safeedge
DB_PASSWORD=safeedge
```

No environment variables are required for the local profile.

### Start backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Unix:

```bash
cd backend
./gradlew bootRun
```

The log should show `The following 1 profile is active: "local"` and connect to `jdbc:postgresql://localhost:5433/safeedge`.

Automated tests:

```powershell
cd backend
.\gradlew.bat clean test
```

`clean test` uses Testcontainers PostgreSQL and does **not** use the Compose database.

Liquibase applies schema migrations when the backend starts. Do not add SQL init scripts to Compose.

### Manual full Tippmix collection

Keep `local` and add `manual-collection` (do not omit `local`):

```powershell
cd backend
.\gradlew.bat bootRun -PspringProfiles=local,manual-collection
```

Unix:

```bash
cd backend
./gradlew bootRun -PspringProfiles=local,manual-collection
```

This starts one collection run at startup against the local database. It calls Tippmix.

### Manual Tippmix result collection

Keep `local` and add `manual-results` (do not omit `local`):

```powershell
cd backend
.\gradlew.bat bootRun "-PspringProfiles=local,manual-results"
```

Unix:

```bash
cd backend
./gradlew bootRun -PspringProfiles=local,manual-results
```

This starts one result collection run at startup. It calls Tippmix. There is no automatic result scheduler yet.

## DBeaver

Connect after Compose is up. Tables appear after the backend has started once (Liquibase).

```text
Host: localhost
Port: 5433
Database: safeedge
Username: safeedge
Password: safeedge
```

## Local frontend

```bash
cd frontend
npm install
npm start
```

Production frontend build:

```bash
cd frontend
npm run build
```

Health check, once the backend is running against PostgreSQL:

```text
GET /actuator/health
```

## Repository layout

```text
SafeEdge/
├── backend/          Spring Boot application
├── frontend/         Angular application
├── docs/             architecture and discovery notes
├── compose.yaml      local PostgreSQL
└── .cursor/rules/    engineering guardrails
```
