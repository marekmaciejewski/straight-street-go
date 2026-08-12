# Project Notes For Agents

This repository contains the backend phase of a simulated car rental system. Follow the sibling-project style from `discount-coupons-management`: non-reactive Spring MVC, OpenAPI-generated API interfaces and DTOs, service-layer business logic, JPA persistence, Liquibase migrations, Docker-based Render deployment, SonarQube Cloud analysis, and RestAssured integration tests.

## Scope

- Keep this phase backend-only.
- Do not add frontend configuration unless explicitly requested.
- Use Java 21 and Maven.
- Use integration tests only for requirement proof; prefer RestAssured and `@Sql`.

## Commands

- `.\mvnw.cmd test` runs the RestAssured `*IT` suite.
- `.\mvnw.cmd verify` also generates JaCoCo reports under `target/site/jacoco`.
- `.\mvnw.cmd spring-boot:run` starts the application locally on the configured `PORT`, defaulting to `8080`.

## Business Rules

- Supported car types are `SEDAN`, `SUV`, and `VAN`.
- The seeded fleet has 2 Sedans, 2 SUVs, and 1 Van.
- Reservations assign a concrete car, not only aggregate capacity.
- Reservation intervals start at `pickupDateTime` and last `numberOfDays`.
- A 1-hour turnaround buffer is applied after `returnDateTime`.
- A car is available again at `returnDateTime + 1 hour`; the blocked interval is end-exclusive.
- Conflict messages should not imply occupancy until the requested return time. They should report the next actual available pickup date.

## SonarQube MCP

When SonarQube MCP tools are available, use them for code-quality context instead of guessing from badges.

Resolve the project key from `pom.xml`:

```text
marekmaciejewski_straight-street-go
```

Use `branch: "master"` for the long-lived branch unless the user explicitly asks for a pull request or another branch.
Do not pass a git branch name as `pullRequest`; SonarQube PR keys are different.

A useful first check is `get_project_quality_gate_status` with the project key and `branch: "master"`. Treat any
remembered result as point-in-time only and recheck when quality status matters.

## Deployment

- `Dockerfile` is the Render runtime image definition.
- `render.yaml` is the Render Free Blueprint config.
- The hosted service uses ephemeral in-memory H2 storage; restarts and redeploys recreate the seeded fleet and erase reservations.
