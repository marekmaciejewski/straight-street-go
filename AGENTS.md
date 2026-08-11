# Project Notes For Agents

This repository contains the backend phase of a simulated car rental system. Follow the sibling-project style from `discount-coupons-management`: non-reactive Spring MVC, OpenAPI-generated API interfaces and DTOs, service-layer business logic, JPA persistence, Liquibase migrations, and RestAssured integration tests.

## Scope

- Keep this phase backend-only.
- Do not add frontend, deployment, or Sonar configuration unless explicitly requested.
- Use Java 21 and Maven.
- Use integration tests only for requirement proof; prefer RestAssured and `@Sql`.

## Business Rules

- Supported car types are `SEDAN`, `SUV`, and `VAN`.
- The seeded fleet has 2 Sedans, 2 SUVs, and 1 Van.
- Reservations assign a concrete car, not only aggregate capacity.
- Reservation intervals start at `pickupDateTime` and last `numberOfDays`.
- A 1-hour turnaround buffer is applied after `returnDateTime`.
- A car is available again at `returnDateTime + 1 hour`; the blocked interval is end-exclusive.
- Conflict messages should not imply occupancy until the requested return time. They should report the next actual available pickup date.

## Verification

- `.\mvnw.cmd test` runs the RestAssured `*IT` suite.
- `.\mvnw.cmd verify` also generates JaCoCo reports under `target/site/jacoco`.
