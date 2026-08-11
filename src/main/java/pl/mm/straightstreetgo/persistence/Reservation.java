package pl.mm.straightstreetgo.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.mm.straightstreetgo.domain.ReservationPeriod;

import java.time.Instant;

@Entity
@Table(name = "RESERVATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CAR_ID", nullable = false)
    private Car car;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 128)
    private String customerId;

    @Column(name = "PICKUP_AT", nullable = false)
    private Instant pickupAt;

    @Column(name = "RETURN_AT", nullable = false)
    private Instant returnAt;

    @Column(name = "RESERVED_UNTIL_AT", nullable = false)
    private Instant reservedUntilAt;

    @Column(name = "NUMBER_OF_DAYS", nullable = false)
    private int numberOfDays;

    private Reservation(Car car, String customerId, ReservationPeriod period) {
        this.car = car;
        this.customerId = customerId;
        this.pickupAt = period.pickupAt();
        this.returnAt = period.returnAt();
        this.reservedUntilAt = period.reservedUntilAt();
        this.numberOfDays = period.numberOfDays();
    }

    public static Reservation create(Car car, String customerId, ReservationPeriod period) {
        return new Reservation(car, customerId, period);
    }
}
