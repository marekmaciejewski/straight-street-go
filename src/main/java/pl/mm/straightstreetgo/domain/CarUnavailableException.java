package pl.mm.straightstreetgo.domain;

public class CarUnavailableException extends RuntimeException {

    public CarUnavailableException(String message) {
        super(message);
    }
}
