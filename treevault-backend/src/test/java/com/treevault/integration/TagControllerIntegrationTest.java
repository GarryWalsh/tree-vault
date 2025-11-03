package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.TagRequest;
import com.treevault.domain.model.valueobject.NodeType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TagController.
 * Tests the full stack including controller, use cases, domain, and database persistence.
 */
class TagControllerIntegrationTest extends BaseIntegrationTest {

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
        // Delete all nodes to ensure clean state for next test
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM nodes");
    }

    @Test
    @DisplayName("Should add tag successfully and persist to database")
    void shouldAddTagSuccessfully() {
        // Given
        String nodeId = createFolder("TaggedFolder");

        TagRequest request = new TagRequest();
        request.setKey("priority");
        request.setValue("high");

        // When
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201)
                .body("key", equalTo("priority"))
                .body("value", equalTo("high"));

        // Then - Verify database state
        Integer tagCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid AND tag_key = ? AND tag_value = ?",
                Integer.class,
                nodeId, "priority", "high"
        );
        assert tagCount != null && tagCount == 1;

        // Verify tag appears in tree
        given()
                .when()
                .get("/tree")
                .then()
                .statusCode(200)
                .body("root.children.find { it.id == '" + nodeId + "' }.tags.priority", equalTo("high"));
    }

    @Test
    @DisplayName("Should remove tag successfully and delete from database")
    void shouldRemoveTagSuccessfully() {
        // Given - Create node and add tag
        String nodeId = createFolder("TaggedFolder");
        addTag(nodeId, "status", "active");

        // Verify tag exists in database
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                Integer.class,
                nodeId, "status"
        );
        assert countBefore != null && countBefore == 1;

        // When
        given()
                .when()
                .delete("/nodes/{id}/tags/{key}", nodeId, "status")
                .then()
                .statusCode(204);

        // Then - Verify deleted from database
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                Integer.class,
                nodeId, "status"
        );
        assert countAfter != null && countAfter == 0;
    }

    @Test
    @DisplayName("Should fail when adding tag with empty key")
    void shouldFailWhenAddingTagWithEmptyKey() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("");
        request.setValue("value");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(400);

        // Verify no tag was persisted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert count != null && count == 0;
    }

    @Test
    @DisplayName("Should fail when adding tag with null key")
    void shouldFailWhenAddingTagWithNullKey() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey(null);
        request.setValue("value");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when adding tag with null value")
    void shouldFailWhenAddingTagWithNullValue() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("key");
        request.setValue(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when adding tag with empty value")
    void shouldFailWhenAddingTagWithEmptyValue() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("status");
        request.setValue("");

        // Empty value is rejected by @NotBlank validation on TagRequest
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when adding tag with very long key")
    void shouldFailWhenAddingTagWithVeryLongKey() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("a".repeat(101)); // Exceeds max length
        request.setValue("value");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(422); // Domain validation error

        // Verify no tag was persisted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert count != null && count == 0;
    }

    @Test
    @DisplayName("Should fail when adding tag with very long value")
    void shouldFailWhenAddingTagWithVeryLongValue() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("key");
        request.setValue("a".repeat(501)); // Exceeds max length

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(422); // Domain validation error

        // Verify no tag was persisted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert count != null && count == 0;
    }

    @Test
    @DisplayName("Should handle tag with special characters and persist correctly")
    void shouldHandleTagWithSpecialCharacters() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("app_env");
        request.setValue("prod:us-west-1");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201)
                .body("key", equalTo("app_env"))
                .body("value", equalTo("prod:us-west-1"));

        // Verify database state
        String dbValue = jdbcTemplate.queryForObject(
                "SELECT tag_value FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                String.class,
                nodeId, "app_env"
        );
        assert "prod:us-west-1".equals(dbValue);
    }

    @Test
    @DisplayName("Should handle unicode characters in tag value and persist correctly")
    void shouldHandleUnicodeCharactersInTagValue() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("location");
        request.setValue("東京 🗼");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201)
                .body("key", equalTo("location"))
                .body("value", equalTo("東京 🗼"));

        // Verify database state with unicode
        String dbValue = jdbcTemplate.queryForObject(
                "SELECT tag_value FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                String.class,
                nodeId, "location"
        );
        assert "東京 🗼".equals(dbValue);
    }

    @Test
    @DisplayName("Should fail when removing tag from invalid node ID")
    void shouldFailWhenRemovingTagFromInvalidNodeId() {
        given()
                .when()
                .delete("/nodes/invalid-uuid/tags/somekey")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should handle removing tag with special characters in key")
    void shouldHandleRemovingTagWithSpecialCharactersInKey() {
        // Given
        String nodeId = createFolder("Node");
        addTag(nodeId, "app_env", "production");

        // When
        given()
                .when()
                .delete("/nodes/{id}/tags/{key}", nodeId, "app_env")
                .then()
                .statusCode(204);

        // Then - Verify deleted from database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                Integer.class,
                nodeId, "app_env"
        );
        assert count != null && count == 0;
    }

    @Test
    @DisplayName("Should fail when request body is malformed")
    void shouldFailWhenRequestBodyIsMalformed() {
        String nodeId = createFolder("Node");

        given()
                .contentType(ContentType.JSON)
                .body("{invalid json")
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail when content type is missing for add tag")
    void shouldFailWhenContentTypeIsMissingForAddTag() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("key");
        request.setValue("value");

        given()
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(415); // Unsupported Media Type
    }

    @Test
    @DisplayName("Should normalize tag key to lowercase and persist correctly")
    void shouldNormalizeTagKeyToLowercase() {
        String nodeId = createFolder("Node");

        TagRequest request = new TagRequest();
        request.setKey("UPPERCASE");
        request.setValue("value");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201)
                .body("key", equalTo("uppercase")); // Normalized to lowercase

        // Verify database state - key should be lowercase
        String dbKey = jdbcTemplate.queryForObject(
                "SELECT tag_key FROM tags WHERE node_id = ?::uuid",
                String.class,
                nodeId
        );
        assert "uppercase".equals(dbKey);
    }

    @Test
    @DisplayName("Should enforce 50 tag limit per node")
    void shouldEnforce50TagLimitPerNode() {
        String nodeId = createFolder("NodeWithManyTags");

        // Add 50 tags (the limit)
        for (int i = 0; i < 50; i++) {
            addTag(nodeId, "key" + i, "value" + i);
        }

        // Verify 50 tags exist
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert count != null && count == 50;

        // Try to add the 51st tag - should fail
        TagRequest request = new TagRequest();
        request.setKey("key51");
        request.setValue("value51");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(422); // Domain validation error

        // Verify still only 50 tags
        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
                Integer.class,
                nodeId
        );
        assert finalCount != null && finalCount == 50;
    }

    @Test
    @DisplayName("Should overwrite existing tag with same key and update database")
    void shouldOverwriteExistingTagWithSameKey() {
        // Given
        String nodeId = createFolder("Node");
        addTag(nodeId, "status", "old");

        // Verify initial value
        String oldValue = jdbcTemplate.queryForObject(
                "SELECT tag_value FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                String.class,
                nodeId, "status"
        );
        assert "old".equals(oldValue);

        // When - Add tag with same key but different value
        TagRequest request = new TagRequest();
        request.setKey("status");
        request.setValue("new");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201)
                .body("key", equalTo("status"))
                .body("value", equalTo("new"));

        // Then - Verify updated in database (not duplicated)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                Integer.class,
                nodeId, "status"
        );
        assert count != null && count == 1; // Still only one tag

        String newValue = jdbcTemplate.queryForObject(
                "SELECT tag_value FROM tags WHERE node_id = ?::uuid AND tag_key = ?",
                String.class,
                nodeId, "status"
        );
        assert "new".equals(newValue);
    }

    // Helper methods

    private String createFolder(String name) {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName(name);
        request.setType(NodeType.FOLDER);
        request.setParentId(null);

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

    private void addTag(String nodeId, String key, String value) {
        TagRequest request = new TagRequest();
        request.setKey(key);
        request.setValue(value);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/nodes/{id}/tags", nodeId)
                .then()
                .statusCode(201);
    }
}

