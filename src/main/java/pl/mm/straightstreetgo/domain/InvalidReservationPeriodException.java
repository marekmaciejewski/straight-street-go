package pl.mm.straightstreetgo.domain;

public class InvalidReservationPeriodException extends RuntimeException {

    public InvalidReservationPeriodException(String message) {
        super(message);
    }
}
