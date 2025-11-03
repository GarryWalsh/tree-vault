package com.treevault.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for API mapping and DTOs.
 * Tests the full request/response cycle including ApiMapper.
 */
class ApiMappingIntegrationTest extends BaseIntegrationTest {

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

    @Nested
    @DisplayName("Node Response Mapping Tests")
    class NodeResponseMappingTests {

        @Test
        @DisplayName("Should return complete node response with all fields")
        void shouldReturnCompleteNodeResponseWithAllFields() {
            // Given - Create a folder
            String folderId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "TestFolder",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Verify all fields are present and correctly formatted
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root", notNullValue())
                    .body("root.id", notNullValue())
                    .body("root.name", equalTo("root"))
                    .body("root.type", equalTo("FOLDER"))
                    .body("root.path", notNullValue())
                    .body("root.createdAt", notNullValue())
                    .body("root.updatedAt", notNullValue())
                    .body("root.version", notNullValue())
                    .body("root.tags", notNullValue())
                    .body("root.children", notNullValue())
                    .body("root.children.size()", equalTo(1))
                    .body("root.children[0].id", equalTo(folderId))
                    .body("root.children[0].name", equalTo("TestFolder"))
                    .body("root.children[0].parentId", equalTo(given().get("/tree").path("root.id").toString()));
        }

        @Test
        @DisplayName("Should map node with tags correctly")
        void shouldMapNodeWithTagsCorrectly() {
            // Given - Create a node
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "TaggedNode",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // Add tags
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "priority",
                            "value": "high"
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(201);

            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "category",
                            "value": "important"
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(201);

            // When/Then - Verify tags are mapped correctly
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children.find { it.id == '" + nodeId + "' }.tags.priority", equalTo("high"))
                    .body("root.children.find { it.id == '" + nodeId + "' }.tags.category", equalTo("important"))
                    .body("root.children.find { it.id == '" + nodeId + "' }.tags.size()", equalTo(2));
        }

        @Test
        @DisplayName("Should map nested structure correctly")
        void shouldMapNestedStructureCorrectly() {
            // Given - Create nested structure
            String level1Id = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Level1",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            String level2Id = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Level2",
                            "type": "FOLDER",
                            "parentId": "%s"
                        }
                        """.formatted(level1Id))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            String level3Id = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Level3",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(level2Id))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Verify nested mapping
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children[0].name", equalTo("Level1"))
                    .body("root.children[0].children[0].name", equalTo("Level2"))
                    .body("root.children[0].children[0].children[0].name", equalTo("Level3"))
                    .body("root.children[0].children[0].children[0].type", equalTo("FILE"));
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Given - Create node with special characters
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Name with Spaces & Special_chars-123",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            // When/Then
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children.find { it.name == 'Name with Spaces & Special_chars-123' }", notNullValue());
        }

        @Test
        @DisplayName("Should handle unicode characters in names")
        void shouldHandleUnicodeCharactersInNames() {
            // Given - Create node with unicode
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "文件夹 📁",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            // When/Then
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children.find { it.name == '文件夹 📁' }", notNullValue());
        }

        @Test
        @DisplayName("Should map position field correctly for multiple siblings")
        void shouldMapPositionFieldCorrectlyForMultipleSiblings() {
            // Given - Create multiple siblings
            String parentId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Parent",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Child1",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(parentId))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Child2",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(parentId))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Child3",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(parentId))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            // When/Then - Verify positions are sequential
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children[0].children[0].position", equalTo(0))
                    .body("root.children[0].children[1].position", equalTo(1))
                    .body("root.children[0].children[2].position", equalTo(2));
        }
    }

    @Nested
    @DisplayName("Tag Response Mapping Tests")
    class TagResponseMappingTests {

        @Test
        @DisplayName("Should map tag with special characters")
        void shouldMapTagWithSpecialCharacters() {
            // Given
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Node",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "app_env",
                            "value": "prod:us-west-1"
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(201)
                    .body("key", equalTo("app_env"))
                    .body("value", equalTo("prod:us-west-1"));
        }

        @Test
        @DisplayName("Should map tag with unicode characters")
        void shouldMapTagWithUnicodeCharacters() {
            // Given
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Node",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "location",
                            "value": "東京 🗼"
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(201)
                    .body("key", equalTo("location"))
                    .body("value", equalTo("東京 🗼"));
        }

        @Test
        @DisplayName("Should reject tag with empty value")
        void shouldRejectTagWithEmptyValue() {
            // Given
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Node",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Empty value is rejected by @NotBlank validation
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "status",
                            "value": ""
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should normalize tag key to lowercase")
        void shouldNormalizeTagKeyToLowercase() {
            // Given
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Node",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Key should be normalized to lowercase
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "key": "UPPERCASE",
                            "value": "value"
                        }
                        """)
                    .when()
                    .post("/nodes/{id}/tags", nodeId)
                    .then()
                    .statusCode(201)
                    .body("key", equalTo("uppercase"));
        }
    }

    @Nested
    @DisplayName("Tree Response Mapping Tests")
    class TreeResponseMappingTests {

        @Test
        @DisplayName("Should map empty tree")
        void shouldMapEmptyTree() {
            // When/Then - Initial empty tree
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root", notNullValue())
                    .body("root.name", equalTo("root"))
                    .body("root.type", equalTo("FOLDER"))
                    .body("root.children", empty());
        }

        @Test
        @DisplayName("Should map tree with complex structure")
        void shouldMapTreeWithComplexStructure() {
            // Given - Create a complex tree structure
            String folder1Id = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Folder1",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            String folder2Id = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "Folder2",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // Add children to Folder1
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "File1",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(folder1Id))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            // Add children to Folder2
            String subFolderId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "SubFolder",
                            "type": "FOLDER",
                            "parentId": "%s"
                        }
                        """.formatted(folder2Id))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "File2",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(subFolderId))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201);

            // When/Then - Verify complete tree structure is mapped
            given()
                    .when()
                    .get("/tree")
                    .then()
                    .statusCode(200)
                    .body("root.children.size()", equalTo(2))
                    .body("root.children.find { it.name == 'Folder1' }.children.size()", equalTo(1))
                    .body("root.children.find { it.name == 'Folder2' }.children.size()", equalTo(1))
                    .body("root.children.find { it.name == 'Folder2' }.children[0].children.size()", equalTo(1));
        }
    }

    @Nested
    @DisplayName("Update and Move Response Mapping Tests")
    class UpdateAndMoveResponseMappingTests {

        @Test
        @DisplayName("Should map updated node correctly")
        void shouldMapUpdatedNodeCorrectly() {
            // Given
            String nodeId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "OldName",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Update should return updated node with new name
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "NewName"
                        }
                        """)
                    .when()
                    .put("/nodes/{id}", nodeId)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(nodeId))
                    .body("name", equalTo("NewName"))
                    .body("updatedAt", notNullValue());
        }

        @Test
        @DisplayName("Should map moved node correctly")
        void shouldMapMovedNodeCorrectly() {
            // Given - Create structure for moving
            String sourceParentId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "SourceFolder",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            String targetParentId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "TargetFolder",
                            "type": "FOLDER",
                            "parentId": null
                        }
                        """)
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            String fileId = given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "name": "FileToMove",
                            "type": "FILE",
                            "parentId": "%s"
                        }
                        """.formatted(sourceParentId))
                    .when()
                    .post("/nodes")
                    .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // When/Then - Move should return updated node with new parent
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "newParentId": "%s",
                            "position": 0
                        }
                        """.formatted(targetParentId))
                    .when()
                    .post("/nodes/{id}/move", fileId)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(fileId))
                    .body("name", equalTo("FileToMove"))
                    .body("parentId", equalTo(targetParentId))
                    .body("position", equalTo(0));
        }
    }
}

