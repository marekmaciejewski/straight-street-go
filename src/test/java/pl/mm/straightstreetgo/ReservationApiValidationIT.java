package pl.mm.straightstreetgo;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/clear-reservations.sql")
class ReservationApiValidationIT {

    @LocalServerPort
    private int port;

    @Test
    void createReservation_returnsBadRequestForMissingAndInvalidFields() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "numberOfDays": 0,
                          "customerId": " "
                        }
                        """)
        .when()
                .post("/reservations")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request validation failed"))
                .body("instance", equalTo("/reservations"))
                .body("errors.field", hasItems("carType", "pickupDateTime", "numberOfDays", "customerId"));
    }

    @Test
    void createReservation_returnsBadRequestForTooLongCustomerId() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "carType": "SUV",
                          "pickupDateTime": "2026-09-10T08:00:00Z",
                          "numberOfDays": 1,
                          "customerId": "%s"
                        }
                        """.formatted("x".repeat(129)))
        .when()
                .post("/reservations")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request validation failed"))
                .body("instance", equalTo("/reservations"))
                .body("errors.field", hasItem("customerId"));
    }

    @Test
    void createReservation_returnsBadRequestForMalformedBody() {
        request()
                .contentType(ContentType.JSON)
                .body("{")
        .when()
                .post("/reservations")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request body is invalid"))
                .body("instance", equalTo("/reservations"));
    }

    @Test
    void createReservation_returnsBadRequestForUnsupportedCarType() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "carType": "COUPE",
                          "pickupDateTime": "2026-09-10T08:00:00Z",
                          "numberOfDays": 1,
                          "customerId": "customer-1"
                        }
                        """)
        .when()
                .post("/reservations")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request body is invalid"))
                .body("instance", equalTo("/reservations"));
    }

    @Test
    void createReservation_returnsBadRequestForInvalidPickupDateTime() {
        request()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "carType": "VAN",
                          "pickupDateTime": "tomorrow",
                          "numberOfDays": 1,
                          "customerId": "customer-1"
                        }
                        """)
        .when()
                .post("/reservations")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request body is invalid"))
                .body("instance", equalTo("/reservations"));
    }

    @Test
    void getReservation_returnsBadRequestForReservationIdBelowMinimum() {
        request()
        .when()
                .get("/reservations/{reservationId}", 0)
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request validation failed"))
                .body("instance", equalTo("/reservations/0"));
    }

    @Test
    void getReservation_returnsBadRequestForInvalidReservationId() {
        request()
        .when()
                .get("/reservations/{reservationId}", "abc")
        .then()
                .statusCode(400)
                .header("Content-Type", startsWith("application/problem+json"))
                .body("detail", equalTo("Request path parameter is invalid"))
                .body("instance", equalTo("/reservations/abc"));
    }

    private RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port);
    }
}
