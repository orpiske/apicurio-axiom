package io.apicurio.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the Action Types REST API endpoints.
 */
@QuarkusTest
class ActionResourceTest {

    private static final String ACTION_TYPES_PATH = "/api/v1/action-types";

    @Test
    void testSeedDataLoaded() {
        given()
            .when()
                .get(ACTION_TYPES_PATH)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size()", greaterThanOrEqualTo(9))
                .body("name", hasItems("analyze", "implement", "review", "close-project"));
    }

    @Test
    void testGetSeedActionType() {
        // Get the "analyze" action type by listing and finding it
        int id = given()
            .when()
                .get(ACTION_TYPES_PATH)
            .then()
                .statusCode(200)
                .extract().path("find { it.name == 'analyze' }.id");

        given()
            .when()
                .get(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("analyze"))
                .body("executionMode", equalTo("actor"))
                .body("userTriggerable", equalTo(true))
                .body("emitsEvent", equalTo(true));
    }

    @Test
    void testSystemActionType() {
        int id = given()
            .when()
                .get(ACTION_TYPES_PATH)
            .then()
                .statusCode(200)
                .extract().path("find { it.name == 'close-project' }.id");

        given()
            .when()
                .get(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("close-project"))
                .body("executionMode", equalTo("system"))
                .body("userTriggerable", equalTo(false))
                .body("emitsEvent", equalTo(false));
    }

    @Test
    void testCreateCustomActionType() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "custom-action",
                    "description": "A custom action for testing",
                    "executionMode": "actor",
                    "userTriggerable": true,
                    "emitsEvent": false
                }
                """)
            .when()
                .post(ACTION_TYPES_PATH)
            .then()
                .statusCode(200)
                .body("name", equalTo("custom-action"))
                .body("executionMode", equalTo("actor"))
                .body("userTriggerable", equalTo(true))
                .body("emitsEvent", equalTo(false))
                .extract().path("id");

        given()
            .when()
                .get(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("custom-action"));
    }

    @Test
    void testUpdateActionType() {
        int id = createActionType("update-test-action");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "updated-action",
                    "description": "Updated description",
                    "executionMode": "system",
                    "emitsEvent": true
                }
                """)
            .when()
                .put(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("updated-action"))
                .body("description", equalTo("Updated description"))
                .body("executionMode", equalTo("system"))
                .body("emitsEvent", equalTo(true));
    }

    @Test
    void testDeleteActionType() {
        int id = createActionType("delete-test-action");

        given()
            .when()
                .delete(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(204);

        given()
            .when()
                .get(ACTION_TYPES_PATH + "/" + id)
            .then()
                .statusCode(404);
    }

    @Test
    void testGetActionTypeNotFound() {
        given()
            .when()
                .get(ACTION_TYPES_PATH + "/999999")
            .then()
                .statusCode(404);
    }

    private int createActionType(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "executionMode": "actor"
                }
                """, name))
            .when()
                .post(ACTION_TYPES_PATH)
            .then()
                .statusCode(200)
                .extract().path("id");
    }
}
