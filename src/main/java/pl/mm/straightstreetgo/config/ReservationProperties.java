package pl.mm.straightstreetgo.config;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.reservation")
public record ReservationProperties(
        @NotNull
        @DurationMin(seconds = 0, message = "must not be negative")
        Duration turnaroundBuffer) {
}
