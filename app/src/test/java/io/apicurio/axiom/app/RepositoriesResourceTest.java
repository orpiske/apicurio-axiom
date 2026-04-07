package io.apicurio.axiom.app;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the Repositories REST API endpoints.
 */
@QuarkusTest
class RepositoriesResourceTest {

    private static final String REPOS_PATH = "/api/v1/repositories";

    @Test
    void testListRepositoriesEmpty() {
        given()
            .when()
                .get(REPOS_PATH)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testCreateAndGetRepository() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "apicurio-axiom",
                    "owner": "Apicurio",
                    "source": "github",
                    "url": "https://github.com/Apicurio/apicurio-axiom",
                    "pollInterval": 300
                }
                """)
            .when()
                .post(REPOS_PATH)
            .then()
                .statusCode(200)
                .body("name", equalTo("apicurio-axiom"))
                .body("owner", equalTo("Apicurio"))
                .body("source", equalTo("github"))
                .body("url", equalTo("https://github.com/Apicurio/apicurio-axiom"))
                .body("pollInterval", equalTo(300))
                .body("id", notNullValue())
                .extract().path("id");

        given()
            .when()
                .get(REPOS_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("apicurio-axiom"))
                .body("id", equalTo(id));
    }

    @Test
    void testCreateJiraRepository() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "AXIOM",
                    "owner": "Apicurio",
                    "source": "jira",
                    "url": "https://issues.example.com/projects/AXIOM"
                }
                """)
            .when()
                .post(REPOS_PATH)
            .then()
                .statusCode(200)
                .body("source", equalTo("jira"));
    }

    @Test
    void testUpdateRepository() {
        int id = createRepository("update-repo");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "updated-repo",
                    "owner": "NewOwner",
                    "source": "github",
                    "url": "https://github.com/NewOwner/updated-repo",
                    "pollInterval": 600
                }
                """)
            .when()
                .put(REPOS_PATH + "/" + id)
            .then()
                .statusCode(200)
                .body("name", equalTo("updated-repo"))
                .body("owner", equalTo("NewOwner"))
                .body("pollInterval", equalTo(600));
    }

    @Test
    void testDeleteRepository() {
        int id = createRepository("delete-repo");

        given()
            .when()
                .delete(REPOS_PATH + "/" + id)
            .then()
                .statusCode(204);

        given()
            .when()
                .get(REPOS_PATH + "/" + id)
            .then()
                .statusCode(404);
    }

    @Test
    void testGetRepositoryNotFound() {
        given()
            .when()
                .get(REPOS_PATH + "/999999")
            .then()
                .statusCode(404);
    }

    private int createRepository(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "owner": "TestOwner",
                    "source": "github",
                    "url": "https://github.com/TestOwner/%s"
                }
                """, name, name))
            .when()
                .post(REPOS_PATH)
            .then()
                .statusCode(200)
                .extract().path("id");
    }
}
