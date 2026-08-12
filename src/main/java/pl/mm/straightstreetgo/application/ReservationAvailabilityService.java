package pl.mm.straightstreetgo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.mm.straightstreetgo.api.dto.CarType;
import pl.mm.straightstreetgo.domain.ReservationPeriod;
import pl.mm.straightstreetgo.persistence.Car;
import pl.mm.straightstreetgo.persistence.CarRepository;
import pl.mm.straightstreetgo.persistence.ReservationRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationAvailabilityService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    Optional<Car> findAvailableCar(CarType carType, ReservationPeriod period) {
        return carRepository.findByTypeOrderById(carType).stream()
                .filter(car -> isAvailable(car, period))
                .findFirst();
    }

    Instant findNextAvailablePickupAt(CarType carType, ReservationPeriod requestedPeriod) {
        List<Car> cars = carRepository.findByTypeOrderById(carType);
        return findNextAvailablePickupAt(cars, requestedPeriod);
    }

    private boolean isAvailable(Car car, ReservationPeriod period) {
        return !reservationRepository.existsOverlappingReservation(
                car.getId(),
                period.pickupAt(),
                period.reservedUntilAt());
    }

    private Instant findNextAvailablePickupAt(List<Car> cars, ReservationPeriod requestedPeriod) {
        Instant candidatePickupAt = requestedPeriod.pickupAt();
        while (true) {
            ReservationPeriod candidatePeriod = createCandidatePeriodStartingAt(candidatePickupAt, requestedPeriod);
            List<Instant> blockedUntilDates = findOverlappingReservedUntilDates(cars, candidatePeriod);
            if (isAnyCarAvailableForPeriod(cars, blockedUntilDates)) {
                return candidatePickupAt;
            }
            candidatePickupAt = findEarliestBlockedCarAvailableAgainAt(blockedUntilDates);
        }
    }

    private ReservationPeriod createCandidatePeriodStartingAt(Instant pickupAt, ReservationPeriod requestedPeriod) {
        return new ReservationPeriod(
                pickupAt,
                requestedPeriod.numberOfDays(),
                requestedPeriod.turnaroundBuffer());
    }

    private List<Instant> findOverlappingReservedUntilDates(List<Car> cars, ReservationPeriod period) {
        return cars.stream()
                .map(car -> reservationRepository.findEarliestOverlappingReservedUntilAt(
                        car.getId(),
                        period.pickupAt(),
                        period.reservedUntilAt()))
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean isAnyCarAvailableForPeriod(List<Car> cars, List<Instant> blockedUntilDates) {
        return blockedUntilDates.size() < cars.size();
    }

    private Instant findEarliestBlockedCarAvailableAgainAt(List<Instant> blockedUntilDates) {
        return blockedUntilDates.stream()
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }
}
