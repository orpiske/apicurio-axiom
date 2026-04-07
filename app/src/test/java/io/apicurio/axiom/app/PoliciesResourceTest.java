package io.apicurio.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the Policies REST API endpoints.
 */
@QuarkusTest
class PoliciesResourceTest {

    private static final String POLICIES_PATH = "/api/v1/policies";

    @Test
    void testListPoliciesEmpty() {
        given()
            .when()
                .get(POLICIES_PATH)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testCreateAndGetPolicy() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Auto-tag new issues",
                    "guideline": "If the event represents a new issue, perform the Auto-tag action.",
                    "actionType": "auto-tag"
                }
                """)
            .when()
                .post(POLICIES_PATH)
            .then()
                .statusCode(200)
                .body("name", equalTo("Auto-tag new issues"))
                .body("guideline", containsString("Auto-tag"))
                .body("actionType", equalTo("auto-tag"))
                .body("id", notNullValue())
                .extract().path("id");

        given()
            .when()
                .get(POLICIES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("Auto-tag new issues"))
                .body("id", equalTo(id));
    }

    @Test
    void testCreatePolicyWithActorHint() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Analyze with Claude",
                    "guideline": "If the event is a new issue, analyze it.",
                    "actionType": "analyze",
                    "actorHint": "claude-code-agent"
                }
                """)
            .when()
                .post(POLICIES_PATH)
            .then()
                .statusCode(200)
                .body("actorHint", equalTo("claude-code-agent"));
    }

    @Test
    void testUpdatePolicy() {
        int id = createPolicy("Update Policy Test");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Updated Policy",
                    "guideline": "Updated guideline text",
                    "actionType": "respond"
                }
                """)
            .when()
                .put(POLICIES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("Updated Policy"))
                .body("guideline", equalTo("Updated guideline text"))
                .body("actionType", equalTo("respond"));
    }

    @Test
    void testDeletePolicy() {
        int id = createPolicy("Delete Policy Test");

        given()
            .when()
                .delete(POLICIES_PATH + "/" + id)
            .then()
                .statusCode(204);

        given()
            .when()
                .get(POLICIES_PATH + "/" + id)
            .then()
                .statusCode(404);
    }

    @Test
    void testGetPolicyNotFound() {
        given()
            .when()
                .get(POLICIES_PATH + "/999999")
            .then()
                .statusCode(404);
    }

    private int createPolicy(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "guideline": "Test guideline for %s"
                }
                """, name, name))
            .when()
                .post(POLICIES_PATH)
            .then()
                .statusCode(200)
                .extract().path("id");
    }
}
