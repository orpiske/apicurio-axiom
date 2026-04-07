package io.apicurio.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Tests for the Activity Log REST API endpoint.
 */
@QuarkusTest
class ActivityResourceTest {

    @Test
    void testListActivityLog() {
        // Activity log entries are created as side effects of other operations.
        // This test verifies the endpoint returns a valid response.
        given()
            .when()
                .get("/api/v1/activity")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
