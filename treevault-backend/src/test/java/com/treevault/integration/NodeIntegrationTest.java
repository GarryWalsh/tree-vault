package com.treevault.integration;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.domain.model.valueobject.NodeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NodeIntegrationTest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveNode() {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test Folder");
        request.setType(NodeType.FOLDER);
        
        // When - Create node
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/nodes",
            request,
            NodeResponse.class
        );
        
        // Then - Verify creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Test Folder");
        assertThat(createResponse.getBody().getType()).isEqualTo(NodeType.FOLDER);
        
        String nodeId = createResponse.getBody().getId();
        
        // When - Retrieve node
        ResponseEntity<NodeResponse> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/nodes/" + nodeId,
            NodeResponse.class
        );
        
        // Then - Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(nodeId);
        assertThat(getResponse.getBody().getName()).isEqualTo("Test Folder");
    }
    
    @Test
    void shouldReturnProblemDetailForInvalidOperation() {
        // Given - Create a file request without parent
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("orphan.txt");
        request.setType(NodeType.FILE);
        
        // When
        ResponseEntity<org.springframework.http.ProblemDetail> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/nodes",
            request,
            org.springframework.http.ProblemDetail.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Validation Error");
        assertThat(response.getBody().getDetail()).contains("Files must have a parent folder");
        assertThat(response.getBody().getType())
            .isEqualTo(URI.create("https://treevault.com/errors/validation-error"));
    }
    
    @Test
    void shouldGetTreeStructure() {
        // Given - Create root and some nodes
        CreateNodeRequest rootRequest = new CreateNodeRequest();
        rootRequest.setName("root");
        rootRequest.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> rootResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/nodes",
            rootRequest,
            NodeResponse.class
        );
        
        String rootId = rootResponse.getBody().getId();
        
        // When - Get tree
        ResponseEntity<TreeResponse> treeResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/tree",
            TreeResponse.class
        );
        
        // Then
        assertThat(treeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(treeResponse.getBody()).isNotNull();
        assertThat(treeResponse.getBody().getRoot()).isNotNull();
    }
    
    @Test
    void shouldDeleteNode() {
        // Given - Create a node
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("ToDelete");
        request.setType(NodeType.FOLDER);
        
        ResponseEntity<NodeResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/nodes",
            request,
            NodeResponse.class
        );
        
        String nodeId = createResponse.getBody().getId();
        
        // When - Delete node
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/nodes/" + nodeId,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        // Then
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Verify node is deleted
        ResponseEntity<org.springframework.http.ProblemDetail> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/nodes/" + nodeId,
            org.springframework.http.ProblemDetail.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

