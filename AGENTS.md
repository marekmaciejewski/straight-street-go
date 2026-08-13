# AGENTS.md

## Scope

These instructions apply to the whole repository.

## Project Context

This is a simulated car rental system with a Java 21 Spring Boot 4 backend and a React/Vite frontend. The backend uses
non-reactive Spring MVC, OpenAPI-generated API interfaces and DTOs, service-layer business logic, Spring Data JPA
persistence, H2, Liquibase migrations, Docker-based deployment, SonarQube Cloud analysis, and RestAssured integration
tests.

The frontend lives under `frontend` and uses React, TypeScript, Vite, Bootstrap, ESLint, and `openapi-typescript`.

The HTTP contract is defined in `src/main/resources/openapi/car-rental-api.yaml`. Generated API interfaces and DTOs are
build output under `target/generated-sources/openapi` and should not be edited by hand.
Frontend API types are generated under `frontend/src/generated` and should not be edited by hand.

## Commands

Use a shell where `JAVA_HOME` points to a JDK 21 installation before running Windows Maven wrapper commands.

```powershell
# Only needed when the current shell is not already using JDK 21.
$env:JAVA_HOME="<path-to-jdk-21>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

- `.\mvnw.cmd clean verify` is the main validation command.
- `.\mvnw.cmd clean test` runs the RestAssured `*IT` suite without generating the full JaCoCo report.
- `.\mvnw.cmd compile spring-boot:run` starts the application locally on the configured `PORT`, defaulting to `8080`; keep the `compile` goal so generated OpenAPI sources exist on a fresh checkout.
- Frontend commands run from `frontend`; use Node.js 24 and `npm ci` before the first frontend command.
- `npm run lint` validates frontend lint rules.
- `npm run build` generates TypeScript API types, type-checks the frontend, and writes the Vite production build under `frontend/dist`.
- `npm run dev` starts the frontend against the local backend, and `npm run dev:render` starts it against the hosted Render backend.
- On macOS/Linux, run `chmod +x mvnw` once before `./mvnw clean verify` or `./mvnw compile spring-boot:run`.

## Conventions

- Keep source code under the existing `pl.mm.straightstreetgo` package structure.
- Keep Liquibase changes in `src/main/resources/db/changelogs` and include them from `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Keep requirement-proof tests as integration tests; prefer RestAssured and `@Sql` fixtures.
- Keep generated OpenAPI sources out of manual edits, SonarQube analysis, and coverage expectations.
- Keep frontend source under `frontend/src` and static assets under `frontend/public`.
- Regenerate frontend API types with `npm run generate:api` after changing `src/main/resources/openapi/car-rental-api.yaml`.
- Keep frontend API base URL configuration in Vite environment variables; `VITE_API_BASE_URL` selects the backend, and `GITHUB_PAGES=true` controls the GitHub Pages base path.
- Keep backend CORS origins configurable through `APP_CORS_ALLOWED_ORIGINS`; include local Vite origins and the GitHub Pages origin when needed.

## Business Rules

- Supported car types are `SEDAN`, `SUV`, and `VAN`.
- The seeded fleet has 2 Sedans, 2 SUVs, and 1 Van.
- Reservations assign a concrete car, not only aggregate capacity.
- Reservations assign the first available concrete car of the requested type by car id.
- Reservation intervals start at `pickupDateTime` and last `numberOfDays`.
- A configurable turnaround buffer from `app.reservation.turnaround-buffer` is applied after `returnDateTime`; the current default is `PT1H` in `src/main/resources/application.yaml`.
- A car is available again at `returnDateTime + app.reservation.turnaround-buffer`; the blocked interval is end-exclusive.
- Conflict messages should not imply occupancy until the requested return time. They should report the next actual available pickup date.

## SonarQube MCP

When SonarQube MCP tools are available, use them for code-quality context instead of guessing from badges.

Resolve the project key from `pom.xml`:

```text
marekmaciejewski_straight-street-go
```

Use `branch: "master"` for the long-lived branch unless the user explicitly asks for a pull request or another branch. Do not pass a git branch name as `pullRequest`; SonarQube PR keys are different.

A useful first check is `get_project_quality_gate_status` with the project key and `branch: "master"`. Treat any remembered result as point-in-time only and recheck when quality status matters.

SonarQube analysis includes backend sources, frontend TypeScript/React sources, and GitHub Actions workflows. Frontend
generated API types, build output, dependencies, and frontend coverage are excluded until frontend tests are added.

## Deployment

- `Dockerfile` is the Render runtime image definition.
- `.github/workflows/frontend-pages.yml` builds `frontend` and deploys the static Vite output to GitHub Pages.
- The hosted service uses ephemeral in-memory H2 storage; restarts, redeploys, and idle spin-down recreate the seeded fleet and erase reservations.
