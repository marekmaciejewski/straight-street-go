package pl.mm.straightstreetgo.application;

import org.springframework.stereotype.Component;
import pl.mm.straightstreetgo.api.dto.ReservationResponse;
import pl.mm.straightstreetgo.persistence.Reservation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ReservationMapper {

    ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getId());
        response.setCarId(reservation.getCar().getId());
        response.setCarType(reservation.getCar().getType());
        response.setCustomerId(reservation.getCustomerId());
        response.setPickupDateTime(toOffsetDateTime(reservation.getPickupAt()));
        response.setReturnDateTime(toOffsetDateTime(reservation.getReturnAt()));
        response.setAvailableAgainDateTime(toOffsetDateTime(reservation.getReservedUntilAt()));
        response.setNumberOfDays(reservation.getNumberOfDays());
        return response;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
