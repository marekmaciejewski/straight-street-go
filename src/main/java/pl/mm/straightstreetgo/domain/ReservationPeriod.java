package pl.mm.straightstreetgo.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record ReservationPeriod(
        Instant pickupAt,
        int numberOfDays,
        Duration turnaroundBuffer) {

    public ReservationPeriod {
        Objects.requireNonNull(pickupAt, "pickupAt must not be null");
        Objects.requireNonNull(turnaroundBuffer, "turnaroundBuffer must not be null");
        if (numberOfDays < 1) {
            throw new InvalidReservationPeriodException("numberOfDays must be greater than 0");
        }
        if (turnaroundBuffer.isNegative()) {
            throw new InvalidReservationPeriodException("turnaroundBuffer must not be negative");
        }
    }

    public Instant returnAt() {
        return pickupAt.plus(numberOfDays, ChronoUnit.DAYS);
    }

    public Instant reservedUntilAt() {
        return returnAt().plus(turnaroundBuffer);
    }
}
