# SafeEdge

Sports betting analysis and backtesting application.

SafeEdge ingests odds and event data, evaluates betting candidates, and backtests strategies. It does not place bets.

## Current phase

Technical scaffold. Tippmix API discovery. No betting/business functionality yet, and no automated betting.

## Stack

- Backend: Java 21, Spring Boot, Gradle, PostgreSQL, Liquibase, Spring Web, Validation, Data JPA, Actuator
- Frontend: Angular (standalone)
- Tests: JUnit 5, Testcontainers PostgreSQL

## Local build

Backend (Windows):

```powershell
cd backend
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

Backend (Unix):

```bash
cd backend
./gradlew clean test
./gradlew bootRun
```

`bootRun` expects PostgreSQL. Configure it with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` (see `.env.example`). For local defaults, use `--args='--spring.profiles.active=local'` or `SPRING_PROFILES_ACTIVE=local`.

Frontend:

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
└── .cursor/rules/    engineering guardrails
```
