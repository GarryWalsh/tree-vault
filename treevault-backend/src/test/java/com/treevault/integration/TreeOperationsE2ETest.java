package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.UpdateNodeRequest;
import com.treevault.api.dto.request.MoveNodeRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.domain.model.valueobject.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TreeOperationsE2ETest extends BaseIntegrationTest {
    
    @BeforeEach
    void cleanupDatabase() {
        // Clean up test data before each test to prevent test pollution
        // This ensures each test starts with a clean database state
        jdbcTemplate.execute("DELETE FROM tags");
        jdbcTemplate.execute("DELETE FROM nodes");
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
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        
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
        // Given - Get the root node
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        // The root is at depth 0, so we can create up to depth 49 successfully
        // When - Create 49 levels deep (depth 1 to 49)
        String currentParentId = rootId;
        for (int i = 0; i < 49; i++) {
            CreateNodeRequest levelRequest = new CreateNodeRequest();
            levelRequest.setName("Level" + i);
            levelRequest.setType(NodeType.FOLDER);
            levelRequest.setParentId(currentParentId);
            
            ResponseEntity<NodeResponse> levelResponse = restTemplate.postForEntity(
                getBaseUrl() + "/nodes",
                levelRequest,
                NodeResponse.class
            );
            
            assertThat(levelResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            currentParentId = levelResponse.getBody().getId();
        }
        
        // Then - Verify structure
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Attempt to create at depth 50 should fail (exceeds MAX_DEPTH)
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
    
    @Test
    @DisplayName("Bug scenario: Should support renaming the root folder multiple times")
    void shouldRenameRootFolderMultipleTimes() {
        // Given - Get the tree and root node
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rootId = treeResponse.getBody().getRoot().getId();
        
        // When - Rename root folder first time
        UpdateNodeRequest update1 = new UpdateNodeRequest();
        update1.setName("RenamedRoot1");
        
        ResponseEntity<NodeResponse> renameResponse1 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + rootId,
            HttpMethod.PUT,
            new HttpEntity<>(update1),
            NodeResponse.class
        );
        
        // Then - First rename should succeed
        assertThat(renameResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(renameResponse1.getBody().getName()).isEqualTo("RenamedRoot1");
        
        // When - Rename root folder second time
        UpdateNodeRequest update2 = new UpdateNodeRequest();
        update2.setName("RenamedRoot2");
        
        ResponseEntity<NodeResponse> renameResponse2 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + rootId,
            HttpMethod.PUT,
            new HttpEntity<>(update2),
            NodeResponse.class
        );
        
        // Then - Second rename should also succeed
        assertThat(renameResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(renameResponse2.getBody().getName()).isEqualTo("RenamedRoot2");
        
        // When - Rename root folder third time
        UpdateNodeRequest update3 = new UpdateNodeRequest();
        update3.setName("RenamedRoot3");
        
        ResponseEntity<NodeResponse> renameResponse3 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + rootId,
            HttpMethod.PUT,
            new HttpEntity<>(update3),
            NodeResponse.class
        );
        
        // Then - Third rename should also succeed
        assertThat(renameResponse3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(renameResponse3.getBody().getName()).isEqualTo("RenamedRoot3");
        
        // Verify final state in tree
        ResponseEntity<TreeResponse> finalTreeResponse = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(finalTreeResponse.getBody().getRoot().getName()).isEqualTo("RenamedRoot3");
    }
    
    @Test
    @DisplayName("Bug scenario: Should move nested folder back to root level")
    void shouldMoveNestedFolderBackToRootLevel() {
        // Given - Create nested structure: Root > Folder1 > Folder2
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
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
        folder2Request.setParentId(folder1Id);
        
        ResponseEntity<NodeResponse> folder2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder2Request,
            NodeResponse.class
        );
        String folder2Id = folder2Response.getBody().getId();
        
        // When - Move Folder2 back to root level
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(rootId);
        moveRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folder2Id + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then - Move should succeed
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getParentId()).isEqualTo(rootId);
        
        // Verify in tree structure
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        NodeResponse root = finalTree.getBody().getRoot();
        assertThat(root.getChildren()).anyMatch(child -> 
            child.getId().equals(folder2Id) && child.getName().equals("Folder2")
        );
    }
    
    @Test
    @DisplayName("Bug scenario: Should move file from deeply nested to grandparent folder")
    void shouldMoveFileToDifferentParentLevel() {
        // Given - Create structure: Root > Parent > Child > file.txt
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest parentRequest = new CreateNodeRequest();
        parentRequest.setName("ParentFolder");
        parentRequest.setType(NodeType.FOLDER);
        parentRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> parentResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            parentRequest,
            NodeResponse.class
        );
        String parentId = parentResponse.getBody().getId();
        
        CreateNodeRequest childRequest = new CreateNodeRequest();
        childRequest.setName("ChildFolder");
        childRequest.setType(NodeType.FOLDER);
        childRequest.setParentId(parentId);
        
        ResponseEntity<NodeResponse> childResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            childRequest,
            NodeResponse.class
        );
        String childId = childResponse.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("document.txt");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(childId);
        
        ResponseEntity<NodeResponse> fileResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        String fileId = fileResponse.getBody().getId();
        
        // When - Move file from ChildFolder to ParentFolder (up one level)
        MoveNodeRequest moveToParentRequest = new MoveNodeRequest();
        moveToParentRequest.setNewParentId(parentId);
        moveToParentRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveToParentResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveToParentRequest),
            NodeResponse.class
        );
        
        // Then - First move should succeed
        assertThat(moveToParentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveToParentResponse.getBody().getParentId()).isEqualTo(parentId);
        
        // When - Move file from ParentFolder to RootFolder (up another level)
        MoveNodeRequest moveToRootRequest = new MoveNodeRequest();
        moveToRootRequest.setNewParentId(rootId);
        moveToRootRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveToRootResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveToRootRequest),
            NodeResponse.class
        );
        
        // Then - Second move should also succeed
        assertThat(moveToRootResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveToRootResponse.getBody().getParentId()).isEqualTo(rootId);
        
        // Verify final position in tree
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        NodeResponse root = finalTree.getBody().getRoot();
        assertThat(root.getChildren()).anyMatch(child -> 
            child.getId().equals(fileId) && child.getName().equals("document.txt")
        );
    }
    
    @Test
    @DisplayName("Bug scenario: Should create new nodes under different folders after operations")
    void shouldCreateNodesUnderDifferentFoldersAfterOperations() {
        // Given - Create two folders
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest test2Request = new CreateNodeRequest();
        test2Request.setName("test2");
        test2Request.setType(NodeType.FOLDER);
        test2Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> test2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            test2Request,
            NodeResponse.class
        );
        String test2Id = test2Response.getBody().getId();
        
        CreateNodeRequest test6Request = new CreateNodeRequest();
        test6Request.setName("test6");
        test6Request.setType(NodeType.FOLDER);
        test6Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> test6Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            test6Request,
            NodeResponse.class
        );
        String test6Id = test6Response.getBody().getId();
        
        // When - Create files under test2
        CreateNodeRequest file1Request = new CreateNodeRequest();
        file1Request.setName("file1.txt");
        file1Request.setType(NodeType.FILE);
        file1Request.setParentId(test2Id);
        
        ResponseEntity<NodeResponse> file1Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file1Request,
            NodeResponse.class
        );
        
        // Then - Should succeed
        assertThat(file1Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(file1Response.getBody().getParentId()).isEqualTo(test2Id);
        
        // When - Rename test2 folder
        UpdateNodeRequest renameRequest = new UpdateNodeRequest();
        renameRequest.setName("test2-renamed");
        
        ResponseEntity<NodeResponse> renameResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + test2Id,
            HttpMethod.PUT,
            new HttpEntity<>(renameRequest),
            NodeResponse.class
        );
        
        assertThat(renameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // When - Create another file under test2 after rename
        CreateNodeRequest file2Request = new CreateNodeRequest();
        file2Request.setName("file2.txt");
        file2Request.setType(NodeType.FILE);
        file2Request.setParentId(test2Id);
        
        ResponseEntity<NodeResponse> file2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file2Request,
            NodeResponse.class
        );
        
        // Then - Should still succeed
        assertThat(file2Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(file2Response.getBody().getParentId()).isEqualTo(test2Id);
        
        // When - Create folder under test6
        CreateNodeRequest subfolderRequest = new CreateNodeRequest();
        subfolderRequest.setName("subfolder");
        subfolderRequest.setType(NodeType.FOLDER);
        subfolderRequest.setParentId(test6Id);
        
        ResponseEntity<NodeResponse> subfolderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            subfolderRequest,
            NodeResponse.class
        );
        
        // Then - Should succeed
        assertThat(subfolderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(subfolderResponse.getBody().getParentId()).isEqualTo(test6Id);
        
        // When - Create file under test6
        CreateNodeRequest file3Request = new CreateNodeRequest();
        file3Request.setName("file3.txt");
        file3Request.setType(NodeType.FILE);
        file3Request.setParentId(test6Id);
        
        ResponseEntity<NodeResponse> file3Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file3Request,
            NodeResponse.class
        );
        
        // Then - Should succeed
        assertThat(file3Response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(file3Response.getBody().getParentId()).isEqualTo(test6Id);
        
        // Verify final tree structure
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        assertThat(finalTree.getStatusCode()).isEqualTo(HttpStatus.OK);
        NodeResponse root = finalTree.getBody().getRoot();
        
        // Verify test2 has 2 files
        NodeResponse test2Node = root.getChildren().stream()
            .filter(child -> child.getId().equals(test2Id))
            .findFirst()
            .orElseThrow();
        assertThat(test2Node.getChildren()).hasSize(2);
        
        // Verify test6 has 1 folder and 1 file
        NodeResponse test6Node = root.getChildren().stream()
            .filter(child -> child.getId().equals(test6Id))
            .findFirst()
            .orElseThrow();
        assertThat(test6Node.getChildren()).hasSize(2);
    }
    
    @Test
    @DisplayName("Bug scenario: Should handle consecutive operations without state corruption")
    void shouldHandleConsecutiveOperationsWithoutStateCorruption() {
        // Given - Create initial structure
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest folderRequest = new CreateNodeRequest();
        folderRequest.setName("TestFolder");
        folderRequest.setType(NodeType.FOLDER);
        folderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folderRequest,
            NodeResponse.class
        );
        String folderId = folderResponse.getBody().getId();
        
        // When & Then - Perform multiple operations in sequence
        // 1. Rename folder
        UpdateNodeRequest rename1 = new UpdateNodeRequest();
        rename1.setName("Renamed1");
        
        ResponseEntity<NodeResponse> response1 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folderId,
            HttpMethod.PUT,
            new HttpEntity<>(rename1),
            NodeResponse.class
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 2. Create child
        CreateNodeRequest childRequest = new CreateNodeRequest();
        childRequest.setName("Child1");
        childRequest.setType(NodeType.FILE);
        childRequest.setParentId(folderId);
        
        ResponseEntity<NodeResponse> response2 = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            childRequest,
            NodeResponse.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // 3. Rename folder again
        UpdateNodeRequest rename2 = new UpdateNodeRequest();
        rename2.setName("Renamed2");
        
        ResponseEntity<NodeResponse> response3 = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folderId,
            HttpMethod.PUT,
            new HttpEntity<>(rename2),
            NodeResponse.class
        );
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 4. Create another child
        CreateNodeRequest child2Request = new CreateNodeRequest();
        child2Request.setName("Child2");
        child2Request.setType(NodeType.FILE);
        child2Request.setParentId(folderId);
        
        ResponseEntity<NodeResponse> response4 = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            child2Request,
            NodeResponse.class
        );
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Verify final state
        ResponseEntity<NodeResponse> finalResponse = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + folderId,
            NodeResponse.class
        );
        
        assertThat(finalResponse.getBody().getName()).isEqualTo("Renamed2");
        assertThat(finalResponse.getBody().getChildren()).hasSize(2);
    }
    
    @Test
    @DisplayName("Bug scenario: Should support reordering siblings within same parent")
    void shouldReorderSiblingsWithinSameParent() {
        // Given - Create parent with 3 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
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
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder2Request,
            NodeResponse.class
        );
        
        CreateNodeRequest folder3Request = new CreateNodeRequest();
        folder3Request.setName("Folder3");
        folder3Request.setType(NodeType.FOLDER);
        folder3Request.setParentId(rootId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder3Request,
            NodeResponse.class
        );
        
        // When - Move Folder1 to position 2 (after Folder2 and Folder3)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(rootId);
        moveRequest.setPosition(2);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folder1Id + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then - Move should succeed (no name conflict error)
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getParentId()).isEqualTo(rootId);
        assertThat(moveResponse.getBody().getPosition()).isEqualTo(2);
        
        // Verify order in tree
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        NodeResponse root = finalTree.getBody().getRoot();
        assertThat(root.getChildren()).hasSize(3);
        
        // Order should now be: Folder2, Folder3, Folder1
        assertThat(root.getChildren().get(0).getName()).isEqualTo("Folder2");
        assertThat(root.getChildren().get(1).getName()).isEqualTo("Folder3");
        assertThat(root.getChildren().get(2).getName()).isEqualTo("Folder1");
    }
    
    @Test
    @DisplayName("Bug scenario: Should move folder from nested location back to parent")
    void shouldMoveFolderFromNestedToParentLocation() {
        // Given - Create structure: test2 > test6 > subfolder
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest test2Request = new CreateNodeRequest();
        test2Request.setName("test2");
        test2Request.setType(NodeType.FOLDER);
        test2Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> test2Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            test2Request,
            NodeResponse.class
        );
        String test2Id = test2Response.getBody().getId();
        
        CreateNodeRequest test6Request = new CreateNodeRequest();
        test6Request.setName("test6");
        test6Request.setType(NodeType.FOLDER);
        test6Request.setParentId(test2Id);
        
        ResponseEntity<NodeResponse> test6Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            test6Request,
            NodeResponse.class
        );
        String test6Id = test6Response.getBody().getId();
        
        CreateNodeRequest subfolderRequest = new CreateNodeRequest();
        subfolderRequest.setName("subfolder");
        subfolderRequest.setType(NodeType.FOLDER);
        subfolderRequest.setParentId(test6Id);
        
        ResponseEntity<NodeResponse> subfolderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            subfolderRequest,
            NodeResponse.class
        );
        String subfolderId = subfolderResponse.getBody().getId();
        
        // When - Move subfolder from test6 back to test2
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(test2Id);
        moveRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + subfolderId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then - Move should succeed
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getParentId()).isEqualTo(test2Id);
        
        // Verify structure
        ResponseEntity<NodeResponse> test2Node = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + test2Id,
            NodeResponse.class
        );
        
        // test2 should now have 2 children: subfolder and test6
        assertThat(test2Node.getBody().getChildren()).hasSize(2);
        assertThat(test2Node.getBody().getChildren())
            .anyMatch(child -> child.getId().equals(subfolderId) && child.getName().equals("subfolder"))
            .anyMatch(child -> child.getId().equals(test6Id) && child.getName().equals("test6"));
    }
    
    @Test
    @DisplayName("Bug scenario: Should reorder files within folder")
    void shouldReorderFilesWithinFolder() {
        // Given - Create folder with 3 files
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest folderRequest = new CreateNodeRequest();
        folderRequest.setName("Documents");
        folderRequest.setType(NodeType.FOLDER);
        folderRequest.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folderResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folderRequest,
            NodeResponse.class
        );
        String folderId = folderResponse.getBody().getId();
        
        CreateNodeRequest file1Request = new CreateNodeRequest();
        file1Request.setName("file1.txt");
        file1Request.setType(NodeType.FILE);
        file1Request.setParentId(folderId);
        
        ResponseEntity<NodeResponse> file1Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file1Request,
            NodeResponse.class
        );
        String file1Id = file1Response.getBody().getId();
        
        CreateNodeRequest file2Request = new CreateNodeRequest();
        file2Request.setName("file2.txt");
        file2Request.setType(NodeType.FILE);
        file2Request.setParentId(folderId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file2Request,
            NodeResponse.class
        );
        
        CreateNodeRequest file3Request = new CreateNodeRequest();
        file3Request.setName("file3.txt");
        file3Request.setType(NodeType.FILE);
        file3Request.setParentId(folderId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            file3Request,
            NodeResponse.class
        );
        
        // When - Move file1 to end (position 2)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(folderId);
        moveRequest.setPosition(2);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + file1Id + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then - Move should succeed without name conflict
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getPosition()).isEqualTo(2);
        
        // Verify order
        ResponseEntity<NodeResponse> folderNode = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + folderId,
            NodeResponse.class
        );
        
        assertThat(folderNode.getBody().getChildren()).hasSize(3);
        assertThat(folderNode.getBody().getChildren().get(0).getName()).isEqualTo("file2.txt");
        assertThat(folderNode.getBody().getChildren().get(1).getName()).isEqualTo("file3.txt");
        assertThat(folderNode.getBody().getChildren().get(2).getName()).isEqualTo("file1.txt");
    }
    
    @Test
    @DisplayName("Bug scenario: Should handle moving node to beginning of sibling list")
    void shouldMoveNodeToBeginningOfSiblingList() {
        // Given - Create parent with 3 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        CreateNodeRequest folder1Request = new CreateNodeRequest();
        folder1Request.setName("A_First");
        folder1Request.setType(NodeType.FOLDER);
        folder1Request.setParentId(rootId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder1Request,
            NodeResponse.class
        );
        
        CreateNodeRequest folder2Request = new CreateNodeRequest();
        folder2Request.setName("B_Second");
        folder2Request.setType(NodeType.FOLDER);
        folder2Request.setParentId(rootId);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder2Request,
            NodeResponse.class
        );
        
        CreateNodeRequest folder3Request = new CreateNodeRequest();
        folder3Request.setName("C_Third");
        folder3Request.setType(NodeType.FOLDER);
        folder3Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folder3Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder3Request,
            NodeResponse.class
        );
        String folder3Id = folder3Response.getBody().getId();
        
        // When - Move C_Third to position 0 (beginning)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(rootId);
        moveRequest.setPosition(0);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + folder3Id + "/move",
            HttpMethod.POST,
            new HttpEntity<>(moveRequest),
            NodeResponse.class
        );
        
        // Then
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getPosition()).isEqualTo(0);
        
        // Verify order
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        NodeResponse root = finalTree.getBody().getRoot();
        assertThat(root.getChildren().get(0).getName()).isEqualTo("C_Third");
        assertThat(root.getChildren().get(1).getName()).isEqualTo("A_First");
        assertThat(root.getChildren().get(2).getName()).isEqualTo("B_Second");
    }
    
    @Test
    @DisplayName("Bug scenario: Should move node between different parent folders multiple times")
    void shouldMoveNodeBetweenDifferentParentsMultipleTimes() {
        // Given - Create 3 folders and 1 file
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
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
        
        CreateNodeRequest folder3Request = new CreateNodeRequest();
        folder3Request.setName("Folder3");
        folder3Request.setType(NodeType.FOLDER);
        folder3Request.setParentId(rootId);
        
        ResponseEntity<NodeResponse> folder3Response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            folder3Request,
            NodeResponse.class
        );
        String folder3Id = folder3Response.getBody().getId();
        
        CreateNodeRequest fileRequest = new CreateNodeRequest();
        fileRequest.setName("movable.txt");
        fileRequest.setType(NodeType.FILE);
        fileRequest.setParentId(folder1Id);
        
        ResponseEntity<NodeResponse> fileResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            fileRequest,
            NodeResponse.class
        );
        String fileId = fileResponse.getBody().getId();
        
        // When - Move file from Folder1 to Folder2
        MoveNodeRequest move1Request = new MoveNodeRequest();
        move1Request.setNewParentId(folder2Id);
        move1Request.setPosition(0);
        
        ResponseEntity<NodeResponse> move1Response = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(move1Request),
            NodeResponse.class
        );
        
        assertThat(move1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(move1Response.getBody().getParentId()).isEqualTo(folder2Id);
        
        // When - Move file from Folder2 to Folder3
        MoveNodeRequest move2Request = new MoveNodeRequest();
        move2Request.setNewParentId(folder3Id);
        move2Request.setPosition(0);
        
        ResponseEntity<NodeResponse> move2Response = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(move2Request),
            NodeResponse.class
        );
        
        assertThat(move2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(move2Response.getBody().getParentId()).isEqualTo(folder3Id);
        
        // When - Move file back to Folder1
        MoveNodeRequest move3Request = new MoveNodeRequest();
        move3Request.setNewParentId(folder1Id);
        move3Request.setPosition(0);
        
        ResponseEntity<NodeResponse> move3Response = restTemplate.exchange(
            getBaseUrl() + "/nodes/" + fileId + "/move",
            HttpMethod.POST,
            new HttpEntity<>(move3Request),
            NodeResponse.class
        );
        
        // Then - All moves should succeed
        assertThat(move3Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(move3Response.getBody().getParentId()).isEqualTo(folder1Id);
        
        // Verify final state
        ResponseEntity<NodeResponse> folder1Node = restTemplate.getForEntity(
            getBaseUrl() + "/nodes/" + folder1Id,
            NodeResponse.class
        );
        
        assertThat(folder1Node.getBody().getChildren()).hasSize(1);
        assertThat(folder1Node.getBody().getChildren().get(0).getName()).isEqualTo("movable.txt");
    }
    
    @Test
    @DisplayName("Reordering: Should move node to end of sibling list")
    void shouldMoveNodeToEndOfSiblingList() {
        // Given - Create parent with 3 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        String firstId = createFolder("First", rootId);
        createFolder("Second", rootId);
        createFolder("Third", rootId);
        
        // When - Move First to end (position 2)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(rootId);
        moveRequest.setPosition(2);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes/" + firstId + "/move",
            moveRequest,
            NodeResponse.class
        );
        
        // Then - Verify order: Second, Third, First
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moveResponse.getBody().getPosition()).isEqualTo(2);
        
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = finalTree.getBody().getRoot().getChildren();
        assertThat(children).hasSize(3);
        assertThat(children.get(0).getName()).isEqualTo("Second");
        assertThat(children.get(1).getName()).isEqualTo("Third");
        assertThat(children.get(2).getName()).isEqualTo("First");
    }
    
    @Test
    @DisplayName("Reordering: Should reorder middle elements in large sibling list")
    void shouldReorderMiddleElementsInLargeSiblingList() {
        // Given - Create parent with 10 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        String[] ids = new String[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = createFolder("Folder" + i, rootId);
        }
        
        // When - Move Folder7 to position 2 (between Folder1 and Folder2)
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(rootId);
        moveRequest.setPosition(2);
        
        ResponseEntity<NodeResponse> moveResponse = restTemplate.postForEntity(
            getBaseUrl() + "/nodes/" + ids[7] + "/move",
            moveRequest,
            NodeResponse.class
        );
        
        // Then - Verify order
        assertThat(moveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = finalTree.getBody().getRoot().getChildren();
        assertThat(children).hasSize(10);
        assertThat(children.get(0).getName()).isEqualTo("Folder0");
        assertThat(children.get(1).getName()).isEqualTo("Folder1");
        assertThat(children.get(2).getName()).isEqualTo("Folder7");  // Moved here
        assertThat(children.get(3).getName()).isEqualTo("Folder2");
        assertThat(children.get(4).getName()).isEqualTo("Folder3");
    }
    
    @Test
    @DisplayName("Reordering: Should handle consecutive reordering operations")
    void shouldHandleConsecutiveReorderingOperations() {
        // Given - Create parent with 5 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        String aId = createFolder("A", rootId);
        createFolder("B", rootId);
        String cId = createFolder("C", rootId);
        createFolder("D", rootId);
        String eId = createFolder("E", rootId);
        
        // When - Perform multiple reordering operations
        // 1. Move E to position 0: E, A, B, C, D
        moveNode(eId, rootId, 0);
        
        // 2. Move C to position 1: E, C, A, B, D
        moveNode(cId, rootId, 1);
        
        // 3. Move A to position 4: E, C, B, D, A
        moveNode(aId, rootId, 4);
        
        // Then - Verify final order
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = finalTree.getBody().getRoot().getChildren();
        assertThat(children).hasSize(5);
        assertThat(children.get(0).getName()).isEqualTo("E");
        assertThat(children.get(1).getName()).isEqualTo("C");
        assertThat(children.get(2).getName()).isEqualTo("B");
        assertThat(children.get(3).getName()).isEqualTo("D");
        assertThat(children.get(4).getName()).isEqualTo("A");
    }
    
    @Test
    @DisplayName("Reordering: Should reorder mixed files and folders")
    void shouldReorderMixedFilesAndFolders() {
        // Given - Create parent with mixed children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        createFolder("FolderA", rootId);
        String file1Id = createFile("file1.txt", rootId);
        createFolder("FolderB", rootId);
        createFile("file2.txt", rootId);
        
        // When - Move file1 to end
        moveNode(file1Id, rootId, 3);
        
        // Then - Verify order: FolderA, FolderB, file2.txt, file1.txt
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = finalTree.getBody().getRoot().getChildren();
        assertThat(children).hasSize(4);
        assertThat(children.get(0).getName()).isEqualTo("FolderA");
        assertThat(children.get(1).getName()).isEqualTo("FolderB");
        assertThat(children.get(2).getName()).isEqualTo("file2.txt");
        assertThat(children.get(3).getName()).isEqualTo("file1.txt");
    }
    
    @Test
    @DisplayName("Reordering: Should maintain order after deleting middle sibling")
    void shouldMaintainOrderAfterDeletingMiddleSibling() {
        // Given - Create parent with 5 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        createFolder("A", rootId);
        createFolder("B", rootId);
        String cId = createFolder("C", rootId);
        createFolder("D", rootId);
        createFolder("E", rootId);
        
        // When - Delete C (middle element)
        restTemplate.delete(getBaseUrl() + "/nodes/" + cId);
        
        // Then - Verify remaining siblings maintain their relative order
        ResponseEntity<TreeResponse> treeAfterDelete = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = treeAfterDelete.getBody().getRoot().getChildren();
        assertThat(children).hasSize(4);
        assertThat(children.get(0).getName()).isEqualTo("A");
        assertThat(children.get(1).getName()).isEqualTo("B");
        assertThat(children.get(2).getName()).isEqualTo("D");
        assertThat(children.get(3).getName()).isEqualTo("E");
        
        // And - Verify positions are sequential
        assertThat(children.get(0).getPosition()).isEqualTo(0);
        assertThat(children.get(1).getPosition()).isEqualTo(1);
        assertThat(children.get(2).getPosition()).isEqualTo(2);
        assertThat(children.get(3).getPosition()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Reordering: Should swap adjacent siblings")
    void shouldSwapAdjacentSiblings() {
        // Given - Create parent with 4 children
        ResponseEntity<TreeResponse> initialTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        String rootId = initialTree.getBody().getRoot().getId();
        
        createFolder("A", rootId);
        createFolder("B", rootId);
        String cId = createFolder("C", rootId);
        createFolder("D", rootId);
        
        // When - Swap B and C (move C to position 1)
        moveNode(cId, rootId, 1);
        
        // Then - Verify order: A, C, B, D
        ResponseEntity<TreeResponse> finalTree = restTemplate.getForEntity(
            getBaseUrl() + "/tree",
            TreeResponse.class
        );
        
        List<NodeResponse> children = finalTree.getBody().getRoot().getChildren();
        assertThat(children).hasSize(4);
        assertThat(children.get(0).getName()).isEqualTo("A");
        assertThat(children.get(1).getName()).isEqualTo("C");
        assertThat(children.get(2).getName()).isEqualTo("B");
        assertThat(children.get(3).getName()).isEqualTo("D");
    }
    
    // Helper methods for test readability
    private String createFolder(String name, String parentId) {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName(name);
        request.setType(NodeType.FOLDER);
        request.setParentId(parentId);
        
        ResponseEntity<NodeResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            request,
            NodeResponse.class
        );
        
        return response.getBody().getId();
    }
    
    private String createFile(String name, String parentId) {
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName(name);
        request.setType(NodeType.FILE);
        request.setParentId(parentId);
        
        ResponseEntity<NodeResponse> response = restTemplate.postForEntity(
            getBaseUrl() + "/nodes",
            request,
            NodeResponse.class
        );
        
        return response.getBody().getId();
    }
    
    private void moveNode(String nodeId, String newParentId, int position) {
        MoveNodeRequest moveRequest = new MoveNodeRequest();
        moveRequest.setNewParentId(newParentId);
        moveRequest.setPosition(position);
        
        restTemplate.postForEntity(
            getBaseUrl() + "/nodes/" + nodeId + "/move",
            moveRequest,
            NodeResponse.class
        );
    }
}

