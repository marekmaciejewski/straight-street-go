package pl.mm.straightstreetgo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationPropertiesStartupFailureIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StraightStreetGoApplication.class)
            .withPropertyValues("spring.liquibase.enabled=false", "app.reservation.turnaround-buffer=-PT1H");

    @Test
    void startupFailsFast_forInvalidTurnaroundBuffer() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasMessageContaining(
                            "Error creating bean with name 'app.reservation-pl.mm.straightstreetgo.config.ReservationProperties': Could not bind properties to 'ReservationProperties' : prefix=app.reservation")
                    .rootCause()
                    .isExactlyInstanceOf(BindValidationException.class)
                    .hasMessageStartingWith("Binding validation errors on app.reservation")
                    .hasMessageContainingAll(
                            "Field error in object 'app.reservation' on field 'turnaroundBuffer': rejected value [PT-1H];",
                            "default message [must not be negative]");
        });
    }
}
