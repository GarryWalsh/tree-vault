package com.treevault.api.exception;

import com.treevault.domain.exception.CircularReferenceException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.exception.NodeValidationException;
import com.treevault.domain.model.valueobject.NodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    
    private WebRequest createWebRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return new ServletWebRequest(request);
    }
    
    @Test
    @DisplayName("Should handle NodeNotFoundException with correct ProblemDetail format")
    void shouldHandleNodeNotFoundException() {
        // Given
        NodeId nodeId = NodeId.generate();
        NodeNotFoundException ex = new NodeNotFoundException("Node not found: " + nodeId);
        WebRequest request = createWebRequest("/api/v1/nodes/" + nodeId);
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleNodeNotFound(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Node Not Found");
        assertThat(response.getBody().getDetail()).contains("Node not found");
        assertThat(response.getBody().getType()).isEqualTo(URI.create("https://treevault.com/errors/node-not-found"));
        assertThat(response.getBody().getProperties()).containsKey("errorId");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }
    
    @Test
    @DisplayName("Should include nodeId in ProblemDetail when available")
    void shouldIncludeNodeIdInProblemDetail() {
        // Given
        String nodeId = "123e4567-e89b-12d3-a456-426614174000";
        NodeNotFoundException ex = new NodeNotFoundException("Node not found", nodeId);
        WebRequest request = createWebRequest("/api/v1/nodes/" + nodeId);
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleNodeNotFound(ex, request);
        
        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsKey("nodeId");
        assertThat(response.getBody().getProperties().get("nodeId")).isEqualTo(nodeId);
    }
    
    @Test
    @DisplayName("Should handle InvalidNodeOperationException")
    void shouldHandleInvalidNodeOperationException() {
        // Given
        InvalidNodeOperationException ex = new InvalidNodeOperationException("Invalid operation");
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleInvalidOperation(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid Node Operation");
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid operation");
        assertThat(response.getBody().getType()).isEqualTo(URI.create("https://treevault.com/errors/invalid-operation"));
    }
    
    @Test
    @DisplayName("Should handle NodeValidationException")
    void shouldHandleNodeValidationException() {
        // Given
        NodeValidationException ex = new NodeValidationException("Validation failed");
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleValidationError(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getTitle()).isEqualTo("Validation Error");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
        assertThat(response.getBody().getType()).isEqualTo(URI.create("https://treevault.com/errors/validation-error"));
    }
    
    @Test
    @DisplayName("Should include violations in ProblemDetail when available")
    void shouldIncludeViolationsInProblemDetail() {
        // Given
        java.util.Map<String, String> violations = java.util.Map.of(
            "name", "Name is required",
            "type", "Type is invalid"
        );
        NodeValidationException ex = new NodeValidationException("Validation failed", violations);
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleValidationError(ex, request);
        
        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsKey("violations");
        assertThat(response.getBody().getProperties().get("violations")).isEqualTo(violations);
    }
    
    @Test
    @DisplayName("Should handle CircularReferenceException")
    void shouldHandleCircularReferenceException() {
        // Given
        CircularReferenceException ex = new CircularReferenceException("Circular reference detected");
        WebRequest request = createWebRequest("/api/v1/nodes/123/move");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleCircularReference(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Circular Reference Detected");
        assertThat(response.getBody().getDetail()).isEqualTo("Circular reference detected");
        assertThat(response.getBody().getType()).isEqualTo(URI.create("https://treevault.com/errors/circular-reference"));
    }
    
    @Test
    @DisplayName("Should handle generic Exception")
    void shouldHandleGenericException() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error");
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getType()).isEqualTo(URI.create("https://treevault.com/errors/internal-error"));
    }
    
    @Test
    @DisplayName("Should set instance URI correctly")
    void shouldSetInstanceUriCorrectly() {
        // Given
        NodeNotFoundException ex = new NodeNotFoundException("Node not found");
        WebRequest request = createWebRequest("/api/v1/nodes/123");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleNodeNotFound(ex, request);
        
        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).contains("/api/v1/nodes/123");
    }
    
    @Test
    @DisplayName("Should generate unique errorId for each exception")
    void shouldGenerateUniqueErrorIdForEachException() {
        // Given
        NodeNotFoundException ex1 = new NodeNotFoundException("Error 1");
        NodeNotFoundException ex2 = new NodeNotFoundException("Error 2");
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response1 = handler.handleNodeNotFound(ex1, request);
        ResponseEntity<ProblemDetail> response2 = handler.handleNodeNotFound(ex2, request);
        
        // Then
        assertThat(response1.getBody()).isNotNull();
        assertThat(response2.getBody()).isNotNull();
        String errorId1 = (String) response1.getBody().getProperties().get("errorId");
        String errorId2 = (String) response2.getBody().getProperties().get("errorId");
        assertThat(errorId1).isNotEqualTo(errorId2);
    }
    
    @Test
    @DisplayName("Should set timestamp in ProblemDetail")
    void shouldSetTimestampInProblemDetail() {
        // Given
        NodeNotFoundException ex = new NodeNotFoundException("Node not found");
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleNodeNotFound(ex, request);
        
        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
        Object timestamp = response.getBody().getProperties().get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isInstanceOf(java.time.Instant.class);
    }
    
    @Test
    @DisplayName("Should handle exceptions with null message")
    void shouldHandleExceptionsWithNullMessage() {
        // Given
        RuntimeException ex = new RuntimeException();
        WebRequest request = createWebRequest("/api/v1/nodes");
        
        // When
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
    }
}

