package pl.mm.straightstreetgo.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ReservationPeriod(
        Instant pickupAt,
        int numberOfDays,
        Duration turnaroundBuffer) {

    public Instant returnAt() {
        return pickupAt.plus(numberOfDays, ChronoUnit.DAYS);
    }

    public Instant reservedUntilAt() {
        return returnAt().plus(turnaroundBuffer);
    }
}
