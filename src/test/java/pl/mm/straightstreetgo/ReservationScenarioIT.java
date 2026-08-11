package pl.mm.straightstreetgo;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/clear-reservations.sql")
class ReservationScenarioIT {

    @LocalServerPort
    private int port;

    @Test
    void createReservation_assignsFirstAvailableSedanAndCalculatesReturnAndBuffer() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "carType": "SEDAN",
                          "pickupDateTime": "2026-09-01T10:00:00Z",
                          "numberOfDays": 2,
                          "customerId": "customer-1"
                        }
                        """)
        .when()
                .post("/reservations")
        .then()
                .statusCode(201)
                .header("Location", startsWith("/reservations/"))
                .header("Content-Type", startsWith("application/json"))
                .body("reservationId", notNullValue())
                .body("carId", equalTo(1))
                .body("carType", equalTo("SEDAN"))
                .body("customerId", equalTo("customer-1"))
                .body("pickupDateTime", equalTo("2026-09-01T10:00:00Z"))
                .body("returnDateTime", equalTo("2026-09-03T10:00:00Z"))
                .body("availableAgainDateTime", equalTo("2026-09-03T11:00:00Z"))
                .body("numberOfDays", equalTo(2));
    }

    @Test
    void createReservation_respectsLimitedCapacityForOverlappingRequests() {
        createReservation("SEDAN", "2026-09-01T10:00:00Z", 2, "customer-1", 1);
        createReservation("SEDAN", "2026-09-01T10:00:00Z", 2, "customer-2", 2);

        request()
                .contentType(ContentType.JSON)
                .body(reservationJson("SEDAN", "2026-09-01T12:00:00Z", 2, "customer-3"))
        .when()
                .post("/reservations")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo(
                        "No SEDAN is available for the requested reservation period. " +
                                "Next available pickup date is 2026-09-03T11:00:00Z"))
                .body("instance", equalTo("/reservations"));

        createReservation("SEDAN", "2026-09-03T11:00:00Z", 2, "customer-4", 1);
    }

    @Test
    void createReservation_appliesOneHourTurnaroundBuffer() {
        createReservation("VAN", "2026-09-01T10:00:00Z", 1, "customer-1", 5);

        request()
                .contentType(ContentType.JSON)
                .body(reservationJson("VAN", "2026-09-02T10:30:00Z", 1, "customer-2"))
        .when()
                .post("/reservations")
        .then()
                .statusCode(409)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo(
                        "No VAN is available for the requested reservation period. " +
                                "Next available pickup date is 2026-09-02T11:00:00Z"));

        createReservation("VAN", "2026-09-02T11:00:00Z", 1, "customer-3", 5);
    }

    @Test
    void getReservation_returnsCreatedReservation() {
        long reservationId = createReservation("SUV", "2026-09-05T09:15:00Z", 3, "customer-7", 3);

        request()
        .when()
                .get("/reservations/{reservationId}", reservationId)
        .then()
                .statusCode(200)
                .header("Content-Type", startsWith("application/json"))
                .body("reservationId", equalTo((int) reservationId))
                .body("carId", equalTo(3))
                .body("carType", equalTo("SUV"))
                .body("customerId", equalTo("customer-7"))
                .body("pickupDateTime", equalTo("2026-09-05T09:15:00Z"))
                .body("returnDateTime", equalTo("2026-09-08T09:15:00Z"))
                .body("availableAgainDateTime", equalTo("2026-09-08T10:15:00Z"))
                .body("numberOfDays", equalTo(3));
    }

    @Test
    void getReservation_returnsNotFound() {
        request()
        .when()
                .get("/reservations/{reservationId}", 999)
        .then()
                .statusCode(404)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("999 reservation not found"))
                .body("instance", equalTo("/reservations/999"));
    }

    private long createReservation(
            String carType,
            String pickupDateTime,
            int numberOfDays,
            String customerId,
            int expectedCarId) {
        Number reservationId = request()
                .contentType(ContentType.JSON)
                .body(reservationJson(carType, pickupDateTime, numberOfDays, customerId))
        .when()
                .post("/reservations")
        .then()
                .statusCode(201)
                .body("carId", equalTo(expectedCarId))
                .extract()
                .path("reservationId");
        return reservationId.longValue();
    }

    private static String reservationJson(
            String carType,
            String pickupDateTime,
            int numberOfDays,
            String customerId) {
        return """
                {
                  "carType": "%s",
                  "pickupDateTime": "%s",
                  "numberOfDays": %d,
                  "customerId": "%s"
                }
                """.formatted(carType, pickupDateTime, numberOfDays, customerId);
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }
}
