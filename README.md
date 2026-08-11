# Straight Street Go

Straight Street Go is a simulated car rental backend. It exposes a non-reactive Spring MVC API for reserving concrete cars from a limited fleet.

## Current Scope

- Reserve a car of a requested type for a pickup date/time and number of days.
- Supported car types: `SEDAN`, `SUV`, `VAN`.
- Seeded fleet size: 2 Sedans, 2 SUVs, 1 Van.
- Assign the first available concrete car of the requested type.
- Retrieve a reservation by id.
- No cancellation, availability endpoint, frontend, deployment, or Sonar setup in this phase.

## Turnaround Buffer

Reservations include a 1-hour turnaround buffer after the rental return time. A car can be rented again only when this buffer has elapsed.

Example:

- Pickup: `2026-09-01T10:00:00Z`
- Number of days: `2`
- Return: `2026-09-03T10:00:00Z`
- Available again: `2026-09-03T11:00:00Z`

The system rejects requests that overlap either the rental time or the turnaround buffer. If no car of the requested type is available, the conflict response includes the next available pickup date in the `detail` message.

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

```powershell
.\mvnw.cmd test
```

The test suite is intentionally integration-test based and uses RestAssured with `@Sql` fixtures.

## Coverage

Generate the JaCoCo report with:

```powershell
.\mvnw.cmd verify
```

Reports are written to:

- `target/site/jacoco/index.html`
- `target/site/jacoco/jacoco.xml`
- `target/site/jacoco/jacoco.csv`
