# AGENTS.md

## Scope

These instructions apply to the whole repository.

## Project Context

This is the backend phase of a simulated car rental system. It is a Java 21 Spring Boot 4 application using non-reactive
Spring MVC, OpenAPI-generated API interfaces and DTOs, service-layer business logic, Spring Data JPA persistence, H2,
Liquibase migrations, Docker-based deployment, SonarQube Cloud analysis, and RestAssured integration tests.

The HTTP contract is defined in `src/main/resources/openapi/car-rental-api.yaml`. Generated API interfaces and DTOs are
build output under `target/generated-sources/openapi` and should not be edited by hand.

The next phase will add a frontend. Until that phase starts explicitly, keep this repository backend-only and do not add
frontend configuration.

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
- On macOS/Linux, run `chmod +x mvnw` once before `./mvnw clean verify` or `./mvnw compile spring-boot:run`.

## Conventions

- Keep source code under the existing `pl.mm.straightstreetgo` package structure.
- Keep Liquibase changes in `src/main/resources/db/changelogs` and include them from `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Keep requirement-proof tests as integration tests; prefer RestAssured and `@Sql` fixtures.
- Keep generated OpenAPI sources out of manual edits, SonarQube analysis, and coverage expectations.
- Document any future frontend commands separately when the frontend phase starts.

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

## Deployment

- `Dockerfile` is the Render runtime image definition.
- The hosted service uses ephemeral in-memory H2 storage; restarts, redeploys, and idle spin-down recreate the seeded fleet and erase reservations.
