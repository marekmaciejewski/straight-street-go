package pl.mm.straightstreetgo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.straightstreetgo.api.dto.CarType;
import pl.mm.straightstreetgo.api.dto.ReservationCreateRequest;
import pl.mm.straightstreetgo.api.dto.ReservationResponse;
import pl.mm.straightstreetgo.config.ReservationProperties;
import pl.mm.straightstreetgo.domain.CarUnavailableException;
import pl.mm.straightstreetgo.domain.ReservationNotFoundException;
import pl.mm.straightstreetgo.domain.ReservationPeriod;
import pl.mm.straightstreetgo.persistence.Car;
import pl.mm.straightstreetgo.persistence.Reservation;
import pl.mm.straightstreetgo.persistence.ReservationRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationAvailabilityService reservationAvailabilityService;
    private final ReservationMapper reservationMapper;
    private final ReservationProperties reservationProperties;

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        ReservationPeriod period = new ReservationPeriod(
                request.getPickupDateTime().toInstant(),
                request.getNumberOfDays(),
                reservationProperties.turnaroundBuffer());
        Car car = reservationAvailabilityService.findAvailableCar(request.getCarType(), period)
                .orElseThrow(() -> unavailable(request.getCarType(), period));
        Reservation reservation = Reservation.create(car, request.getCustomerId().trim(), period);
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(long reservationId) {
        return reservationRepository.findById(reservationId)
                .map(reservationMapper::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId + " reservation not found"));
    }

    private CarUnavailableException unavailable(CarType carType, ReservationPeriod requestedPeriod) {
        Instant nextAvailablePickupAt =
                reservationAvailabilityService.findNextAvailablePickupAt(carType, requestedPeriod);
        return new CarUnavailableException(
                "No %s is available for the requested reservation period. Next available pickup date is %s"
                        .formatted(carType, nextAvailablePickupAt));
    }
}
