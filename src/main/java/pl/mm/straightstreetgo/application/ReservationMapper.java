package pl.mm.straightstreetgo.application;

import org.springframework.stereotype.Component;
import pl.mm.straightstreetgo.api.dto.ReservationResponse;
import pl.mm.straightstreetgo.domain.CarType;
import pl.mm.straightstreetgo.persistence.Reservation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ReservationMapper {

    CarType toDomainCarType(pl.mm.straightstreetgo.api.dto.CarType carType) {
        return CarType.valueOf(carType.name());
    }

    ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getId());
        response.setCarId(reservation.getCar().getId());
        response.setCarType(toApiCarType(reservation.getCar().getType()));
        response.setCustomerId(reservation.getCustomerId());
        response.setPickupDateTime(toOffsetDateTime(reservation.getPickupAt()));
        response.setReturnDateTime(toOffsetDateTime(reservation.getReturnAt()));
        response.setAvailableAgainDateTime(toOffsetDateTime(reservation.getReservedUntilAt()));
        response.setNumberOfDays(reservation.getNumberOfDays());
        return response;
    }

    private static pl.mm.straightstreetgo.api.dto.CarType toApiCarType(CarType carType) {
        return pl.mm.straightstreetgo.api.dto.CarType.valueOf(carType.name());
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
