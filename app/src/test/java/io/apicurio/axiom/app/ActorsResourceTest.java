package io.apicurio.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the Actors REST API endpoints.
 */
@QuarkusTest
class ActorsResourceTest {

    private static final String ACTORS_PATH = "/api/v1/actors";

    @Test
    void testListActors() {
        given()
            .when()
                .get(ACTORS_PATH)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testCreateAndGetActor() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Claude Code Agent",
                    "description": "AI agent powered by Claude Code",
                    "type": "ai-agent",
                    "capabilities": ["analyze", "implement", "review"]
                }
                """)
            .when()
                .post(ACTORS_PATH)
            .then()
                .statusCode(200)
                .body("name", equalTo("Claude Code Agent"))
                .body("description", equalTo("AI agent powered by Claude Code"))
                .body("type", equalTo("ai-agent"))
                .body("capabilities", hasItems("analyze", "implement", "review"))
                .body("id", notNullValue())
                .extract().path("id");

        given()
            .when()
                .get(ACTORS_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("Claude Code Agent"))
                .body("id", equalTo(id));
    }

    @Test
    void testCreateHumanActor() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Eric",
                    "description": "Human developer",
                    "type": "human"
                }
                """)
            .when()
                .post(ACTORS_PATH)
            .then()
                .statusCode(200)
                .body("name", equalTo("Eric"))
                .body("type", equalTo("human"));
    }

    @Test
    void testUpdateActor() {
        int id = createActor("Update Actor", "ai-agent");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Updated Agent",
                    "type": "ai-agent",
                    "capabilities": ["analyze", "propose"]
                }
                """)
            .when()
                .put(ACTORS_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("Updated Agent"))
                .body("capabilities", hasItems("analyze", "propose"));
    }

    @Test
    void testDeleteActor() {
        int id = createActor("Delete Actor", "human");

        given()
            .when()
                .delete(ACTORS_PATH + "/" + id)
            .then()
                .statusCode(204);

        given()
            .when()
                .get(ACTORS_PATH + "/" + id)
            .then()
                .statusCode(404);
    }

    @Test
    void testGetActorNotFound() {
        given()
            .when()
                .get(ACTORS_PATH + "/999999")
            .then()
                .statusCode(404);
    }

    private int createActor(String name, String type) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "type": "%s"
                }
                """, name, type))
            .when()
                .post(ACTORS_PATH)
            .then()
                .statusCode(200)
                .extract().path("id");
    }
}
