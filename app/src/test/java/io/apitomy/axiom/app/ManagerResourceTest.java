package io.apitomy.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Tests for the Manager debug REST endpoint.
 * Verifies endpoint routing and error handling only — actual AI Manager
 * invocation requires Claude Code CLI and is tested separately.
 */
@QuarkusTest
class ManagerResourceTest {

    @Test
    void testEvaluateNonexistentEventReturns404() {
        given()
            .when()
                .post("/api/v1/manager/evaluate/999999")
            .then()
                .statusCode(404);
    }
}
