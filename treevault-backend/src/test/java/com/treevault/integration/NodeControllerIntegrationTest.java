package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.MoveNodeRequest;
import com.treevault.api.dto.request.UpdateNodeRequest;
import com.treevault.domain.model.valueobject.NodeType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for NodeController.
 * Tests the full stack including controller, use cases, domain, and database persistence.
 */
class NodeControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @AfterEach
    void cleanupDatabase() {
        // Clean up test data to prevent test pollution
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM nodes WHERE name != 'root'");
    }

    @Test
    @DisplayName("Should create node successfully and persist to database")
    void shouldCreateNodeSuccessfully() {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test Folder");
        request.setType(NodeType.FOLDER);
        request.setParentId(null);

        // When
        String nodeId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Folder"))
                .body("type", equalTo("FOLDER"))
                .body("id", notNullValue())
                .body("path", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract()
                .path("id");

        // Then - Verify database state
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nodes WHERE id = ?::uuid AND name = ? AND type = 'FOLDER'",
                Integer.class,
                nodeId, "Test Folder"
        );
        assert count != null && count == 1;
    }

    @Test
    @DisplayName("Should get tree successfully")
    void shouldGetTreeSuccessfully() {
        // Given - Create a node first
        String folderId = createFolder("MyFolder", null);

        // When
        given()
                .when()
                .get("/tree")
                .then()
                .statusCode(200)
                .body("root", notNullValue())
                .body("root.name", equalTo("root"))
                .body("root.type", equalTo("FOLDER"))
                .body("root.children.size()", equalTo(1))
                .body("root.children[0].id", equalTo(folderId))
                .body("root.children[0].name", equalTo("MyFolder"));

        // Then - Verify database state (tree structure persisted)
        Integer nodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nodes WHERE name = 'MyFolder'",
                Integer.class
        );
        assert nodeCount != null && nodeCount == 1;
    }

    @Test
    @DisplayName("Should update node successfully and persist changes")
    void shouldUpdateNodeSuccessfully() {
        // Given
        String nodeId = createFolder("OldName", null);

        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName("NewName");

        // When
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/nodes/{id}", nodeId)
                .then()
                .statusCode(200)
                .body("name", equalTo("NewName"))
                .body("id", equalTo(nodeId));

        // Then - Verify database state
        String dbName = jdbcTemplate.queryForObject(
                "SELECT name FROM nodes WHERE id = ?::uuid",
                String.class,
                nodeId
        );
        assert "NewName".equals(dbName);
    }

    @Test
    @DisplayName("Should delete node successfully and remove from database")
    void shouldDeleteNodeSuccessfully() {
        // Given
        String nodeId = createFolder("ToDelete", null);

        // Verify it exists
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nodes WHERE id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert countBefore != null && countBefore == 1;

        // When
        given()
                .when()
                .delete("/nodes/{id}", nodeId)
                .then()
                .statusCode(204);

        // Then - Verify deleted from database
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nodes WHERE id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert countAfter != null && countAfter == 0;
    }

    @Test
    @DisplayName("Should move node successfully and update database relationships")
    void shouldMoveNodeSuccessfully() {
        // Given
        String sourceParentId = createFolder("SourceFolder", null);
        String targetParentId = createFolder("TargetFolder", null);
        String fileId = createFile("FileToMove", sourceParentId);

        MoveNodeRequest request = new MoveNodeRequest();
        request.setNewParentId(targetParentId);
        request.setPosition(0);

        // When
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/move", fileId)
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("parentId", equalTo(targetParentId))
                .body("position", equalTo(0));

        // Then - Verify database state
        String dbParentId = jdbcTemplate.queryForObject(
                "SELECT parent_id::text FROM nodes WHERE id = ?::uuid",
                String.class,
                fileId
        );
        assert targetParentId.equals(dbParentId);
    }

    @Test
    @DisplayName("Should fail when invalid JSON is provided")
    void shouldFailWhenInvalidJsonIsProvided() {
        given()
                .contentType(ContentType.JSON)
                .body("{invalid json}")
                .when()
                .post("/nodes")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when required fields are missing")
    void shouldFailWhenRequiredFieldsAreMissing() {
        // Given - Missing name field
        CreateNodeRequest request = new CreateNodeRequest();
        request.setType(NodeType.FOLDER);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when node type is null")
    void shouldFailWhenNodeTypeIsNull() {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test");
        request.setType(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when update request has empty name")
    void shouldFailWhenUpdateRequestHasEmptyName() {
        // Given
        String nodeId = createFolder("ValidName", null);

        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName("");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/nodes/{id}", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when update request has null name")
    void shouldFailWhenUpdateRequestHasNullName() {
        // Given
        String nodeId = createFolder("ValidName", null);

        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/nodes/{id}", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when move request has invalid position")
    void shouldFailWhenMoveRequestHasInvalidPosition() {
        // Given
        String nodeId = createFolder("Node", null);
        String parentId = createFolder("Parent", null);

        MoveNodeRequest request = new MoveNodeRequest();
        request.setNewParentId(parentId);
        request.setPosition(-1); // Invalid negative position

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/move", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when move request has null parent")
    void shouldFailWhenMoveRequestHasNullParent() {
        // Given
        String nodeId = createFolder("Node", null);

        MoveNodeRequest request = new MoveNodeRequest();
        request.setNewParentId(null);
        request.setPosition(0);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/move", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when node ID is invalid UUID format")
    void shouldFailWhenNodeIdIsInvalidUuidFormat() {
        given()
                .when()
                .delete("/nodes/invalid-uuid")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when request body is empty")
    void shouldFailWhenRequestBodyIsEmpty() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/nodes")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when request has extremely long name")
    void shouldFailWhenRequestHasExtremelyLongName() {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("a".repeat(256)); // Exceeds max length
        request.setType(NodeType.FOLDER);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(400); // DTO validation catches this before domain

        // Then - Verify nothing was persisted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nodes WHERE name LIKE 'aaa%'",
                Integer.class
        );
        assert count != null && count == 0;
    }

    @Test
    @DisplayName("Should fail when content type is missing")
    void shouldFailWhenContentTypeIsMissing() {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test");
        request.setType(NodeType.FOLDER);

        given()
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(415); // Unsupported Media Type
    }

    @Test
    @DisplayName("Should fail when request has malformed JSON structure")
    void shouldFailWhenRequestHasMalformedJsonStructure() {
        given()
                .contentType(ContentType.JSON)
                .body("{invalid json}")
                .when()
                .post("/nodes")
                .then()
                .statusCode(400);
    }

    // Helper methods

    private String createFolder(String name, String parentId) {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName(name);
        request.setType(NodeType.FOLDER);
        request.setParentId(parentId);

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String createFile(String name, String parentId) {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName(name);
        request.setType(NodeType.FILE);
        request.setParentId(parentId);

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
