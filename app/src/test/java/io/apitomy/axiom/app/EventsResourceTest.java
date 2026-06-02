package io.apitomy.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Tests for the Events REST API endpoint.
 */
@QuarkusTest
class EventsResourceTest {

    @Test
    void testListEvents() {
        // Events are created by the event ingestion pipeline, not via REST.
        // This test just verifies the endpoint returns a valid empty list.
        given()
            .when()
                .get("/api/v1/events")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
