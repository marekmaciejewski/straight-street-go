package pl.mm.straightstreetgo.api.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import pl.mm.straightstreetgo.api.ReservationsApi;
import pl.mm.straightstreetgo.api.dto.ReservationCreateRequest;
import pl.mm.straightstreetgo.api.dto.ReservationResponse;
import pl.mm.straightstreetgo.application.ReservationService;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class ReservationController implements ReservationsApi {

    private final ReservationService reservationService;

    @Override
    public ResponseEntity<ReservationResponse> createReservation(@Valid ReservationCreateRequest reservationCreateRequest) {
        ReservationResponse response = reservationService.createReservation(reservationCreateRequest);
        return ResponseEntity
                .created(reservationUri(response.getReservationId()))
                .body(response);
    }

    @Override
    public ResponseEntity<ReservationResponse> getReservation(Long reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }

    private static URI reservationUri(long reservationId) {
        return URI.create("/reservations/" + reservationId);
    }
}
