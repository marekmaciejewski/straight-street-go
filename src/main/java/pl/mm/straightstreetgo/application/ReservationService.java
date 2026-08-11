package pl.mm.straightstreetgo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.straightstreetgo.api.dto.ReservationCreateRequest;
import pl.mm.straightstreetgo.api.dto.ReservationResponse;
import pl.mm.straightstreetgo.config.ReservationProperties;
import pl.mm.straightstreetgo.domain.CarType;
import pl.mm.straightstreetgo.domain.CarUnavailableException;
import pl.mm.straightstreetgo.domain.ReservationNotFoundException;
import pl.mm.straightstreetgo.domain.ReservationPeriod;
import pl.mm.straightstreetgo.persistence.Car;
import pl.mm.straightstreetgo.persistence.CarRepository;
import pl.mm.straightstreetgo.persistence.Reservation;
import pl.mm.straightstreetgo.persistence.ReservationRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationProperties reservationProperties;

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        CarType carType = reservationMapper.toDomainCarType(request.getCarType());
        ReservationPeriod period = new ReservationPeriod(
                request.getPickupDateTime().toInstant(),
                request.getNumberOfDays(),
                reservationProperties.turnaroundBuffer());
        Car car = findAvailableCar(carType, period);
        Reservation reservation = Reservation.create(car, request.getCustomerId().trim(), period);
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(long reservationId) {
        return reservationRepository.findById(reservationId)
                .map(reservationMapper::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId + " reservation not found"));
    }

    private Car findAvailableCar(CarType carType, ReservationPeriod period) {
        List<Car> cars = carRepository.findByTypeOrderById(carType);
        return cars.stream()
                .filter(car -> isAvailable(car, period))
                .findFirst()
                .orElseThrow(() -> unavailable(carType, cars, period));
    }

    private boolean isAvailable(Car car, ReservationPeriod period) {
        return !reservationRepository.existsOverlappingReservation(
                car.getId(),
                period.pickupAt(),
                period.reservedUntilAt());
    }

    private CarUnavailableException unavailable(CarType carType, List<Car> cars, ReservationPeriod requestedPeriod) {
        Instant nextAvailablePickupAt = nextAvailablePickupAt(cars, requestedPeriod);
        return new CarUnavailableException(
                "No %s is available for the requested reservation period. Next available pickup date is %s"
                        .formatted(carType, nextAvailablePickupAt));
    }

    private Instant nextAvailablePickupAt(List<Car> cars, ReservationPeriod requestedPeriod) {
        Instant candidate = requestedPeriod.pickupAt();
        while (true) {
            ReservationPeriod candidatePeriod = new ReservationPeriod(
                    candidate,
                    requestedPeriod.numberOfDays(),
                    requestedPeriod.turnaroundBuffer());
            List<Instant> blockedUntilDates = overlappingReservedUntilDates(cars, candidatePeriod);
            if (blockedUntilDates.size() < cars.size()) {
                return candidate;
            }
            candidate = blockedUntilDates.stream()
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
        }
    }

    private List<Instant> overlappingReservedUntilDates(List<Car> cars, ReservationPeriod period) {
        return cars.stream()
                .map(car -> reservationRepository.findEarliestOverlappingReservedUntilAt(
                        car.getId(),
                        period.pickupAt(),
                        period.reservedUntilAt()))
                .flatMap(Optional::stream)
                .toList();
    }
}
