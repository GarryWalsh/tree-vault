package com.treevault.api.exception;

import com.treevault.domain.exception.CircularReferenceException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.exception.NodeValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@SuppressWarnings("null")
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_URI_PREFIX = "https://treevault.com/errors/";
    
    /**
     * Helper method to safely create a URI from a string.
     * URI.create() never returns null - it throws IllegalArgumentException for invalid URIs.
     * This method wraps the call with Objects.requireNonNull to satisfy null-safety checkers.
     */
    private URI createUri(String uriString) {
        return Objects.requireNonNull(URI.create(uriString), "URI cannot be null");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage(), ex);
        
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed: " + errors
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        logger.warn("Missing request parameter: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Missing required parameter: " + ex.getParameterName()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        logger.warn("Unsupported media type: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Content-Type not supported: " + ex.getContentType()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Unsupported Media Type");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        logger.warn("Invalid request body: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid request body: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Invalid Request Body");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.warn("Type mismatch: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid parameter type for '" + ex.getName() + "': " + ex.getMessage()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Invalid Parameter Type");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    
    
    @ExceptionHandler(NodeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNodeNotFound(
            NodeNotFoundException ex, WebRequest request) {
        logger.warn("Node not found: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "node-not-found"));
        problemDetail.setTitle("Node Not Found");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());

        if (ex.getNodeId() != null) {
            problemDetail.setProperty("nodeId", ex.getNodeId());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(InvalidNodeOperationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidOperation(
            InvalidNodeOperationException ex, WebRequest request) {
        logger.warn("Invalid node operation: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "invalid-operation"));
        problemDetail.setTitle("Invalid Node Operation");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
    
    @ExceptionHandler(NodeValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(
            NodeValidationException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            ex.getMessage()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        if (ex.getViolations() != null) {
            problemDetail.setProperty("violations", ex.getViolations());
        }
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }
    
    @ExceptionHandler(CircularReferenceException.class)
    public ResponseEntity<ProblemDetail> handleCircularReference(
            CircularReferenceException ex, WebRequest request) {
        logger.warn("Circular reference detected: {}", ex.getMessage(), ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "circular-reference"));
        problemDetail.setTitle("Circular Reference Detected");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problemDetail.setType(createUri(ERROR_URI_PREFIX + "internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(createUri(request.getDescription(false)));
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}

