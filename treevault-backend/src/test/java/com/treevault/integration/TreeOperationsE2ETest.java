package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.UpdateNodeRequest;
import com.treevault.api.dto.request.MoveNodeRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.domain.model.valueobject.NodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TreeOperationsE2ETest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }
    
    @Test
    @DisplayName("Should perform complete tree operations end-to-end")
    void shouldPerformCompleteTreeOperationsEndToEnd() {
        // Given - Create root folder
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("RootFolder");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        assertThat(rootResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String rootId = rootResponse.getBody().getId();
        
        // When - Create nested structure
        CreateNodeRequest folderRequest = new CreateNodeRequest();
        folderRequest.setName("SubFolder");
        folderRequest.setType(NodeType.FOLDER);
        folderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folderRequest,
            NodeResponse.class
        );
        
        String folderId = folderResponse.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("document.txt");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(folderId);
        
        ResponseEntity<NodeResponse> fileResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        
        String fileId = fileResponse.getBody().getId();
        
        // Then - Verify tree structure
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(treeResponse.getBody()).isNotNull();
        assertThat(treeResponse.getBody().getRoot()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle concurrent modifications")
    void shouldHandleConcurrentModifications() {
        // Given - Create a node
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("ConcurrentTest");
        request.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            request,
            NodeResponse.class
        );
        
        String nodeId = createResponse.getBody().getId();
        
        // When - Update the same node multiple times
        UpdateNodeRequest update1 = new UpdateNodeRequest();
        update1.setName("Updated1");
        
        UpdateNodeRequest update2 = new UpdateNodeRequest();
        update2.setName("Updated2");
        
        ResponseEntity<NodeResponse> updateResponse1 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + nodeId,
            HttpMethod.PUT,
            new HttpEntity<>(update1),
            NodeResponse.class
        );
        
        ResponseEntity<NodeResponse> updateResponse2 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + nodeId,
            HttpMethod.PUT,
            new HttpEntity<>(update2),
            NodeResponse.class
        );
        
        // Then - Both updates should succeed (optimistic locking handles concurrency)
        assertThat(updateResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify final state
        ResponseEntity<NodeResponse> finalResponse = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + nodeId,
            NodeResponse.class
        );
        
        assertThat(finalResponse.getBody().getName()).isEqualTo("Updated2");
    }
    
    @Test
    @DisplayName("Should handle large tree operations")
    void shouldHandleLargeTreeOperations() {
        // Given - Create root
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("LargeTreeRoot");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String rootId = rootResponse.getBody().getId();
        
        // When - Create 100 folders
        for (int i = 0; i < 100; i++) {
            CreateNodeRequest folderRequest = new CreateNodeRequest();
            folderRequest.setName("Folder" + i);
            folderRequest.setType(NodeType.FOLDER);
            folderRequest.setParentId(rootId);
            
            restTemplate.postForEntity(
                getBaseUrl() + "/nodes",
                folderRequest,
                NodeResponse.class
            );
        }
        
        // Then - Verify tree can be retrieved
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(treeResponse.getBody()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle complex move operations")
    void shouldHandleComplexMoveOperations() {
        // Given - Create tree structure
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("MoveRoot");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String rootId = rootResponse.getBody().getId();
        
        CreateNodeRequest folder1Request = new CreateNodeRequest();
        folder1Request.setName("Folder1");
        folder1Request.setType(NodeType.FOLDER);
        folder1Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folder1Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder1Request,
            NodeResponse.class
        );
        
        String folder1Id = folder1Response.getBody().getId();
        
        CreateNodeRequest folder2Request = new CreateNodeRequest();
        folder2Request.setName("Folder2");
        folder2Request.setType(NodeType.FOLDER);
        folder2Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folder2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder2Request,
            NodeResponse.class
        );
        
        String folder2Id = folder2Response.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("file.txt");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(folder1Id);
        
        ResponseEntity<NodeResponse> fileResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        
        String fileId = fileResponse.getBody().getId();
        
        // When - Move file from folder1 to folder2
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(folder2Id);
        moveRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getParentId()).isEqualTo(folder2Id);
    }
    
    @Test
    @DisplayName("Should prevent circular reference in move operations")
    void shouldPreventCircularReferenceInMoveOperations() {
        // Given - Create parent-child structure
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("CircularRoot");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String rootId = rootResponse.getBody().getId();
        
        CreateNodeRequest childRequest = new CreateNodeRequest();
        childRequest.setName("Child");
        childRequest.setType(NodeType.FOLDER);
        childRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> childResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            childRequest,
            NodeResponse.class
        );
        
        String childId = childResponse.getBody().getId();
        
        // When - Try to move parent to child (should fail)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(childId);
        moveRequest.setPosition(0);
        
        ResponseEntity<org.springframework.http.ProblemDetail> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + rootId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            org.springframework.http.ProblemDetail.class
        );
        
        // Then
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(moveResponse.getBody()).isNotNull();
        assertThat(moveResponse.getBody().getTitle()).isEqualTo("Circular Reference Detected");
    }
    
    @Test
    @DisplayName("Should handle tag operations end-to-end")
    void shouldHandleTagOperationsEndToEnd() {
        // Given - Create a node
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("TaggedNode");
        request.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            request,
            NodeResponse.class
        );
        
        String nodeId = createResponse.getBody().getId();
        
        // When - Add tag
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        com.treevault.api.dto.request.TagRequest tagRequest = new com.treevault.api.dto.request.TagRequest();
        tagRequest.setKey("department");
        tagRequest.setValue("engineering");
        
        ResponseEntity<com.treevault.api.dto.response.TagResponse> addTagResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + nodeId + "/tags",
            HttpMethod.POST,
            new HttpEntity<>(tagRequest, headers),
            com.treevault.api.dto.response.TagResponse.class
        );
        
        // Then
        assertThat(addTagResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Verify tag was added
        ResponseEntity<NodeResponse> nodeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + nodeId,
            NodeResponse.class
        );
        
        assertThat(nodeResponse.getBody().getTags()).isNotEmpty();
        
        // Remove tag
        ResponseEntity<Void> removeTagResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + nodeId + "/tags/department",
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        assertThat(removeTagResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
    
    @Test
    @DisplayName("Should handle cascade deletion")
    void shouldHandleCascadeDeletion() {
        // Given - Create tree structure
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("DeleteRoot");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String rootId = rootResponse.getBody().getId();
        
        CreateNodeRequest folderRequest = new CreateNodeRequest();
        folderRequest.setName("ToDelete");
        folderRequest.setType(NodeType.FOLDER);
        folderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folderRequest,
            NodeResponse.class
        );
        
        String folderId = folderResponse.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("file.txt");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(folderId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        
        // When - Delete folder
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folderId,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        // Then
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Verify folder and file are deleted
        ResponseEntity<org.springframework.http.ProblemDetail> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + folderId,
            org.springframework.http.ProblemDetail.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    @DisplayName("Should handle deep nesting up to maximum depth")
    void shouldHandleDeepNestingToMaximumDepth() {
        // Given - Create deep structure
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("DeepRoot");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String currentParentId = rootResponse.getBody().getId();
        
        // When - Create 50 levels deep
        for (int i = 0; i < 50; i++) {
            CreateNodeRequest levelRequest = new CreateNodeRequest();
            levelRequest.setName("Level" + i);
            levelRequest.setType(NodeType.FOLDER);
            levelRequest.setParentId(currentParentId);
            
            ResponseEntity<NodeResponse> levelResponse = restTemplate.postForEntity(
                getBaseUrl() + "/nodes",
                levelRequest,
                NodeResponse.class
            );
            
            currentParentId = levelResponse.getBody().getId();
        }
        
        // Then - Verify structure
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Attempt to exceed maximum depth should fail
        CreateNodeRequest exceedRequest = new CreateNodeRequest();
        exceedRequest.setName("Exceed");
        exceedRequest.setType(NodeType.FOLDER);
        exceedRequest.setParentId(currentParentId);
        
        ResponseEntity<org.springframework.http.ProblemDetail> exceedResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            exceedRequest,
            org.springframework.http.ProblemDetail.class
        );
        
        assertThat(exceedResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

