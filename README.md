# Straight Street Go

| [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=coverage)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go) | [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=bugs)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go) |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Straight Street Go is a simulated car rental backend. It exposes a non-reactive Spring MVC API for reserving concrete
cars from a limited fleet.

## Current Scope

- Reserve a car of a requested type for a pickup date/time and number of days.
- Supported car types: `SEDAN`, `SUV`, `VAN`.
- Seeded fleet size: 2 Sedans, 2 SUVs, 1 Van.
- Assign the first available concrete car of the requested type.
- Retrieve a reservation by id.
- No cancellation, availability endpoint, or frontend in this phase.

## Turnaround Buffer

Reservations include a 1-hour turnaround buffer after the rental return time. A car can be rented again only when this
buffer has elapsed.

Example:

- Pickup: `2026-09-01T10:00:00Z`
- Number of days: `2`
- Return: `2026-09-03T10:00:00Z`
- Available again: `2026-09-03T11:00:00Z`

The system rejects requests that overlap either the rental time or the turnaround buffer. If no car of the requested
type is available, the conflict response includes the next available pickup date in the `detail` message.

## API

The OpenAPI contract is in `src/main/resources/openapi/car-rental-api.yaml`.

### Create Reservation

`POST /reservations`

```json
{
  "carType": "SEDAN",
  "pickupDateTime": "2026-09-01T10:00:00Z",
  "numberOfDays": 2,
  "customerId": "customer-1"
}
```

### Get Reservation

`GET /reservations/{reservationId}`

## Build And Test

Requires Java 21.

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS/Linux:

```sh
./mvnw test
```

The test suite is intentionally integration-test based and uses RestAssured with `@Sql` fixtures.

## Coverage And SonarQube

Generate the JaCoCo report with:

```powershell
.\mvnw.cmd verify
```

Reports are written to:

- `target/site/jacoco/index.html`
- `target/site/jacoco/jacoco.xml`
- `target/site/jacoco/jacoco.csv`

In GitHub Actions, the [Coverage and SonarQube](https://github.com/marekmaciejewski/straight-street-go/actions/workflows/coverage.yml)
workflow shows a coverage table in the job summary, uploads the full HTML report as the `jacoco-coverage-report`
artifact, and publishes analysis to the
[SonarQube Cloud report](https://sonarcloud.io/summary/overall?id=marekmaciejewski_straight-street-go&branch=master).

For the first SonarQube run, create the project in SonarQube Cloud with:

- organization: `marekmaciejewski`
- project key: `marekmaciejewski_straight-street-go`
- main branch: `master`

Then add a GitHub Actions repository secret named `SONAR_TOKEN`.

For local Codex SonarQube MCP access, `.codex/config.toml` expects the same token in a local environment variable named
`SONAR_TOKEN`. Restart the local Codex client after changing that environment variable so the MCP server can read it.

## Run Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```sh
./mvnw spring-boot:run
```

The application starts on [http://localhost:8080](http://localhost:8080) by default.

Useful local URLs:

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [H2 console](http://localhost:8080/h2-console)

## Deployment Notes

The checked-in `Dockerfile` builds the Maven project with JDK 21 and runs the packaged jar on a JRE image. The
checked-in `render.yaml` defines a Docker-based Render Free web service.

Deployment-relevant environment variables:

| variable                    | example                   | purpose                                               |
|-----------------------------|---------------------------|-------------------------------------------------------|
| `PORT`                      | `10000` on Render         | host-provided server port, defaults to `8080` locally |
| `SPRING_H2_CONSOLE_ENABLED` | `false`                   | disables the public H2 console in the hosted service  |
| `JAVA_TOOL_OPTIONS`         | `-XX:MaxRAMPercentage=75` | caps JVM heap relative to container memory            |

See [Render Free Setup](docs/render-free-setup.md) for the step-by-step Render setup guide.

The hosted backend uses ephemeral in-memory H2 storage. Restart, redeploy, or idle spin-down resets reservations and
recreates the seeded fleet.

Expected hosted URL if the Render service keeps the checked-in name:

- [Swagger UI](https://straight-street-go.onrender.com/swagger-ui/index.html)
- API base URL: `https://straight-street-go.onrender.com`

> [!IMPORTANT]
> The first request after inactivity may take about a minute. The backend runs on Render Free and may need to wake up
> before the API responds.

Quick smoke test against the hosted backend:

```powershell
curl.exe https://straight-street-go.onrender.com/v3/api-docs
```
