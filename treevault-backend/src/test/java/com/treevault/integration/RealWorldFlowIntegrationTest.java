package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.TagRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TagResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.domain.model.valueobject.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that simulate real-world user flows, particularly the frontend
 * behavior where the tree is loaded first (auto-creating root if needed), then
 * subsequent operations are performed on that auto-created root.
 * 
 * These tests verify transaction boundaries and data persistence across multiple
 * HTTP requests, which is critical for catching issues like read-only transactions
 * that don't persist data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RealWorldFlowIntegrationTest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }
    
    @BeforeEach
    void setUp() {
        // Ensure clean state - delete all nodes before each test
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM nodes");
    }
    
    @Test
    @DisplayName("Frontend Flow: Should auto-create root on first tree load, then allow creating children")
    void shouldAutoCreateRootAndAllowCreatingChildren() {
        // GIVEN - Empty database (cleaned in setUp)
        Integer nodeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nodes", Integer.class);
        assertThat(nodeCount).isEqualTo(0);
        
        // WHEN - Frontend loads tree for the first time (simulates page load)
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        // THEN - Root should be auto-created and returned
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(treeResponse.getBody()).isNotNull();
        assertThat(treeResponse.getBody().getRoot()).isNotNull();
        assertThat(treeResponse.getBody().getRoot().getName()).isEqualTo("root");
        assertThat(treeResponse.getBody().getRoot().getType()).isEqualTo(NodeType.FOLDER);
        assertThat(treeResponse.getBody().getRoot().getParentId()).isNull();
        
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // CRITICAL: Verify root is persisted in database (transaction was committed)
        Integer persistedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nodes", Integer.class);
        assertThat(persistedCount).isEqualTo(1);
        
        String persistedRootId = jdbcTemplate.queryForObject(
            "SELECT id::text FROM nodes WHERE name = 'root'", 
            String.class
        );
        assertThat(persistedRootId).isEqualTo(rootId);
        
        // WHEN - User clicks on root and creates a child folder (simulates UI interaction)
        CreateNodeRequest createFolderRequest = new CreateNodeRequest();
        createFolderRequest.setName("My Folder");
        createFolderRequest.setType(NodeType.FOLDER);
        createFolderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            createFolderRequest,
            NodeResponse.class
        );
        
        // THEN - Child should be created successfully
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("My Folder");
        assertThat(createResponse.getBody().getParentId()).isEqualTo(rootId);
        
        // Verify both nodes are in database
        Integer totalNodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nodes", Integer.class);
        assertThat(totalNodes).isEqualTo(2);
        
        // WHEN - Reload tree to verify structure
        ResponseEntity<TreeResponse> reloadedTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        // THEN - Tree should show root with one child
        assertThat(reloadedTree.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reloadedTree.getBody().getRoot().getChildren()).hasSize(1);
        assertThat(reloadedTree.getBody().getRoot().getChildren().get(0).getName()).isEqualTo("My Folder");
    }
    
    @Test
    @DisplayName("Frontend Flow: Should handle multiple child creations under auto-created root")
    void shouldHandleMultipleChildrenUnderAutoCreatedRoot() {
        // GIVEN - Load tree to auto-create root
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // WHEN - Create multiple folders under root
        for (int i = 1; i <= 5; i++) {
            CreateNodeRequest request = new CreateNodeRequest();
            request.setName("Folder " + i);
            request.setType(NodeType.FOLDER);
            request.setParentId(rootId);
            
            ResponseEntity<NodeResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/nodes",
                request,
                NodeResponse.class
            );
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
        
        // THEN - All folders should be persisted
        Integer folderCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nodes WHERE parent_id = ?::uuid",
            Integer.class,
            rootId
        );
        assertThat(folderCount).isEqualTo(5);
        
        // Reload tree and verify structure
        ResponseEntity<TreeResponse> reloadedTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(reloadedTree.getBody().getRoot().getChildren()).hasSize(5);
    }
    
    @Test
    @DisplayName("Frontend Flow: Should handle nested folder creation under auto-created root")
    void shouldHandleNestedStructureUnderAutoCreatedRoot() {
        // GIVEN - Load tree to get auto-created root
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // WHEN - Create nested structure: root -> folder1 -> folder2 -> file
        CreateNodeRequest folder1Request = new CreateNodeRequest();
        folder1Request.setName("Documents");
        folder1Request.setType(NodeType.FOLDER);
        folder1Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folder1Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder1Request,
            NodeResponse.class
        );
        String folder1Id = folder1Response.getBody().getId();
        
        CreateNodeRequest folder2Request = new CreateNodeRequest();
        folder2Request.setName("Work");
        folder2Request.setType(NodeType.FOLDER);
        folder2Request.setParentId(folder1Id);
        
        ResponseEntity<NodeResponse> folder2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder2Request,
            NodeResponse.class
        );
        String folder2Id = folder2Response.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("report.pdf");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(folder2Id);
        
        ResponseEntity<NodeResponse> fileResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        
        // THEN - All nodes should be persisted
        assertThat(fileResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        Integer totalNodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nodes", Integer.class);
        assertThat(totalNodes).isEqualTo(4); // root + 2 folders + 1 file
        
        // Verify tree structure
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(finalTree.getBody().getRoot().getChildren()).hasSize(1);
        assertThat(finalTree.getBody().getRoot().getChildren().get(0).getName()).isEqualTo("Documents");
        assertThat(finalTree.getBody().getRoot().getChildren().get(0).getChildren()).hasSize(1);
        assertThat(finalTree.getBody().getRoot().getChildren().get(0).getChildren().get(0).getName()).isEqualTo("Work");
    }
    
    @Test
    @DisplayName("Frontend Flow: Should add tags to nodes created under auto-created root")
    void shouldAddTagsToNodesUnderAutoCreatedRoot() {
        // GIVEN - Load tree and create a folder
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = treeResponse.getBody().getRoot().getId();
        
        CreateNodeRequest folderRequest = new CreateNodeRequest();
        folderRequest.setName("Project");
        folderRequest.setType(NodeType.FOLDER);
        folderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folderRequest,
            NodeResponse.class
        );
        String folderId = folderResponse.getBody().getId();
        
        // WHEN - Add one tag to the folder
        TagRequest tagRequest = new TagRequest();
        tagRequest.setKey("department");
        tagRequest.setValue("engineering");
        
        ResponseEntity<TagResponse> tagResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes/" + folderId + "/tags",
            tagRequest,
            TagResponse.class
        );
        
        // THEN - Tag should be persisted
        assertThat(tagResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        Integer tagCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tags WHERE node_id = ?::uuid",
            Integer.class,
            folderId
        );
        assertThat(tagCount).isEqualTo(1);
        
        // Verify tag is returned in tree
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        NodeResponse projectNode = finalTree.getBody().getRoot().getChildren().get(0);
        assertThat(projectNode.getTags()).hasSize(1);
        assertThat(projectNode.getTags()).containsKey("department");
    }
    
    @Test
    @DisplayName("Transaction Boundary: Root created in one request should be visible in next request")
    void shouldPersistRootAcrossTransactionBoundaries() {
        // GIVEN - First HTTP request to load tree
        ResponseEntity<TreeResponse> firstRequest = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootIdFromFirstRequest = firstRequest.getBody().getRoot().getId();
        
        // WHEN - Second HTTP request to load tree again (new transaction)
        ResponseEntity<TreeResponse> secondRequest = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootIdFromSecondRequest = secondRequest.getBody().getRoot().getId();
        
        // THEN - Should return the same root (not create a new one)
        assertThat(rootIdFromSecondRequest).isEqualTo(rootIdFromFirstRequest);
        
        // Verify only one root exists in database
        Integer rootCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nodes WHERE parent_id IS NULL",
            Integer.class
        );
        assertThat(rootCount).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Transaction Boundary: Node created in one request should be retrievable in next request")
    void shouldPersistNodeAcrossTransactionBoundaries() {
        // GIVEN - Get root
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // WHEN - Create node in first request
        CreateNodeRequest createRequest = new CreateNodeRequest();
        createRequest.setName("Test Node");
        createRequest.setType(NodeType.FOLDER);
        createRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            createRequest,
            NodeResponse.class
        );
        String nodeId = createResponse.getBody().getId();
        
        // THEN - Retrieve node in second request (new transaction)
        ResponseEntity<NodeResponse> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + nodeId,
            NodeResponse.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(nodeId);
        assertThat(getResponse.getBody().getName()).isEqualTo("Test Node");
    }
    
    @Test
    @DisplayName("Edge Case: Should handle tree load, create, delete, tree load cycle")
    void shouldHandleCompleteLifecycleCycle() {
        // GIVEN - Load tree (auto-create root)
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        // WHEN - Create a folder
        CreateNodeRequest createRequest = new CreateNodeRequest();
        createRequest.setName("Temporary");
        createRequest.setType(NodeType.FOLDER);
        createRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            createRequest,
            NodeResponse.class
        );
        String folderId = createResponse.getBody().getId();
        
        // Verify it exists in tree
        ResponseEntity<TreeResponse> treeWithFolder = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        assertThat(treeWithFolder.getBody().getRoot().getChildren()).hasSize(1);
        
        // Delete the folder
        restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folderId,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        // THEN - Reload tree, should show empty root
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(finalTree.getBody().getRoot().getChildren()).isEmpty();
        assertThat(finalTree.getBody().getRoot().getId()).isEqualTo(rootId); // Same root
    }
    
    @Test
    @DisplayName("Error Case: Creating node with non-existent parent should fail with clear error")
    void shouldFailWhenCreatingNodeWithNonExistentParent() {
        // GIVEN - Load tree to ensure root exists
        restTemplate.getForEntity(getBaseUrl() + "/tree", TreeResponse.class);
        
        // WHEN - Try to create node with fake parent ID
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Orphan");
        request.setType(NodeType.FOLDER);
        request.setParentId("00000000-0000-0000-0000-000000000000");
        
        ResponseEntity<org.springframework.http.ProblemDetail> response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            request,
            org.springframework.http.ProblemDetail.class
        );
        
        // THEN - Should fail with 404 and clear message
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Node Not Found");
        assertThat(response.getBody().getDetail()).contains("Parent node not found");
    }
    
    @Test
    @DisplayName("Concurrency: Multiple tree loads should return same root, not create duplicates")
    void shouldNotCreateDuplicateRootsOnConcurrentLoads() {
        // WHEN - Make multiple concurrent requests (simulate multiple browser tabs)
        ResponseEntity<TreeResponse> response1 = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        ResponseEntity<TreeResponse> response2 = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        ResponseEntity<TreeResponse> response3 = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        // THEN - All should return the same root ID
        String rootId1 = response1.getBody().getRoot().getId();
        String rootId2 = response2.getBody().getRoot().getId();
        String rootId3 = response3.getBody().getRoot().getId();
        
        assertThat(rootId1).isEqualTo(rootId2);
        assertThat(rootId2).isEqualTo(rootId3);
        
        // Verify only one root in database
        Integer rootCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nodes WHERE parent_id IS NULL",
            Integer.class
        );
        assertThat(rootCount).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Error Case: Should prevent deletion of root node with clear error message")
    void shouldPreventDeletingRootNode() {
        // GIVEN - Load tree to get root
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // WHEN - Try to delete the root node
        ResponseEntity<org.springframework.http.ProblemDetail> response = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + rootId,
            HttpMethod.DELETE,
            null,
            org.springframework.http.ProblemDetail.class
        );
        
        // THEN - Should fail with clear error message
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid Node Operation");
        assertThat(response.getBody().getDetail()).contains("Cannot delete the root node");
        
        // Verify root still exists in database
        Integer rootCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nodes WHERE id = ?::uuid",
            Integer.class,
            rootId
        );
        assertThat(rootCount).isEqualTo(1);
    }
}

