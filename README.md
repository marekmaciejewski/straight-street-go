# Straight Street Go

| [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=coverage)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go) | [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=bugs)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go)<br>[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_straight-street-go&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_straight-street-go) |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Spring Boot service and React frontend for reserving concrete cars from a limited rental fleet. The backend supports Sedans, SUVs, and Vans, assigns a specific available car, and applies the configured turnaround buffer after each rental before the car can be booked again.

## Live Demo

- [Frontend UI](https://marekmaciejewski.github.io/straight-street-go/)
- [Swagger UI](https://straight-street-go.onrender.com/swagger-ui/index.html)
- API base URL: `https://straight-street-go.onrender.com`

> [!IMPORTANT]
> The first request after inactivity may take about a minute. The backend runs on Render Free and may need to wake up
> before the API responds.

The hosted backend uses ephemeral in-memory H2 storage. Restart, redeploy, or idle spin-down recreates the seeded fleet
and erases reservations.

## Requirements

- JDK 21
- Maven wrapper included in the repository
- Node.js 24 for the optional frontend UI

## Build And Test

On Windows:

```powershell
# Only needed when the current shell is not already using JDK 21.
$env:JAVA_HOME="<path-to-jdk-21>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Skip the `JAVA_HOME` lines when `java -version` already reports JDK 21.

On macOS/Linux:

```sh
chmod +x mvnw
./mvnw clean verify
```

`verify` compiles the application, generates OpenAPI interfaces and DTOs, runs the RestAssured integration test suite,
and writes the JaCoCo report under `target/site/jacoco`.

Frontend UI:

```powershell
cd frontend
npm ci
npm run build
```

The frontend build generates TypeScript API types from `src/main/resources/openapi/car-rental-api.yaml` and writes the
static Vite output under `frontend/dist`.

In GitHub Actions, the [Coverage and SonarQube](https://github.com/marekmaciejewski/straight-street-go/actions/workflows/coverage.yml)
workflow shows a coverage table in the job summary, uploads the full HTML report as the `jacoco-coverage-report`
artifact, and publishes analysis to the
[SonarQube Cloud report](https://sonarcloud.io/summary/overall?id=marekmaciejewski_straight-street-go&branch=master).
The analysis includes backend sources, frontend TypeScript/React sources, and GitHub Actions workflows. Frontend
generated API types, build output, dependencies, and frontend coverage are excluded until frontend tests are added.

The [Frontend Pages](https://github.com/marekmaciejewski/straight-street-go/actions/workflows/frontend-pages.yml)
workflow builds the Vite app from `frontend` and deploys it to GitHub Pages.

## Run Locally

On Windows:

```powershell
# Only needed when the current shell is not already using JDK 21.
$env:JAVA_HOME="<path-to-jdk-21>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd compile spring-boot:run
```

Skip the `JAVA_HOME` lines when `java -version` already reports JDK 21.

On macOS/Linux:

```sh
chmod +x mvnw
./mvnw compile spring-boot:run
```

The `compile` goal ensures generated OpenAPI sources exist before Spring Boot starts from a fresh checkout. The
application starts on [http://localhost:8080](http://localhost:8080) by default.

Useful local URLs:

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [H2 console](http://localhost:8080/h2-console)

Run the frontend locally against the local backend:

```powershell
cd frontend
npm run dev
```

Run the frontend locally against the hosted Render backend:

```powershell
cd frontend
npm run dev:render
```

## API

- `POST /reservations` creates a reservation for the requested car type.
- `GET /reservations/{reservationId}` reads a reservation by id.

Supported car types are `SEDAN`, `SUV`, and `VAN`. The seeded fleet contains 2 Sedans, 2 SUVs, and 1 Van. A reservation
starts at `pickupDateTime`, lasts `numberOfDays`, and exposes both `returnDateTime` and `availableAgainDateTime`.
The turnaround buffer is configured by `app.reservation.turnaround-buffer`, currently defaulting to `PT1H` in
`src/main/resources/application.yaml`.

The application rejects requests that overlap an existing rental or its turnaround buffer. When every car of the
requested type is blocked, the conflict response reports the next available pickup date.

Create reservation example:

```json
{
  "carType": "SEDAN",
  "pickupDateTime": "2026-09-01T10:00:00Z",
  "numberOfDays": 2,
  "customerId": "customer-1"
}
```

The OpenAPI contract lives in [src/main/resources/openapi/car-rental-api.yaml](src/main/resources/openapi/car-rental-api.yaml).
Generated API interfaces and DTOs are produced under `target/generated-sources/openapi`.

Quick smoke test against the hosted backend:

```powershell
curl.exe https://straight-street-go.onrender.com/v3/api-docs
```

## Deployment Notes

The checked-in `Dockerfile` builds the Maven project with JDK 21 and runs the packaged jar on a JRE image. For Render,
create a Web Service from this repository, select the Free instance type, and use Docker as the runtime.

Deployment-relevant environment variables:

| variable                            | example                                                    | purpose                                               |
|-------------------------------------|------------------------------------------------------------|-------------------------------------------------------|
| `PORT`                              | provided by Render                                         | host-provided server port, defaults to `8080` locally |
| `SPRING_H2_CONSOLE_ENABLED`         | `false`                                                    | disables the public H2 console in the hosted service  |
| `JAVA_TOOL_OPTIONS`                 | `-XX:MaxRAMPercentage=75`                                  | caps JVM heap relative to container memory            |
| `APP_RESERVATION_TURNAROUND_BUFFER` | `PT1H`                                                     | car cleanup buffer after the calculated return time   |
| `APP_CORS_ALLOWED_ORIGINS`          | `https://marekmaciejewski.github.io,http://localhost:5173` | comma-separated frontend origins allowed by CORS      |
