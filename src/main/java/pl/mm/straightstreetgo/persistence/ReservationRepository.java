package pl.mm.straightstreetgo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select count(reservation) > 0
            from Reservation reservation
            where reservation.car.id = :carId
              and reservation.pickupAt < :reservedUntilAt
              and :pickupAt < reservation.reservedUntilAt
            """)
    boolean existsOverlappingReservation(
            @Param("carId") Long carId,
            @Param("pickupAt") Instant pickupAt,
            @Param("reservedUntilAt") Instant reservedUntilAt);

    @Query("""
            select min(reservation.reservedUntilAt)
            from Reservation reservation
            where reservation.car.id = :carId
              and reservation.pickupAt < :reservedUntilAt
              and :pickupAt < reservation.reservedUntilAt
            """)
    Optional<Instant> findEarliestOverlappingReservedUntilAt(
            @Param("carId") Long carId,
            @Param("pickupAt") Instant pickupAt,
            @Param("reservedUntilAt") Instant reservedUntilAt);
}
