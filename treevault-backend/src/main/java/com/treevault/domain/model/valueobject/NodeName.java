package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class NodeName {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 255;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[^/\\\\:*?\"<>|]+$");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    private final String value;
    
    private NodeName(String value) {
        this.value = value;
    }
    
    public static NodeName of(String value) {
        validateName(value);
        String normalized = normalizeWhitespace(value.trim());
        return new NodeName(normalized);
    }
    
    private static void validateName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new NodeValidationException("Node name cannot be null or empty");
        }
        
        String trimmed = value.trim();
        
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new NodeValidationException(
                String.format("Node name must be between %d and %d characters", MIN_LENGTH, MAX_LENGTH)
            );
        }
        
        // Check for control characters (0x00-0x1F, excluding tab 0x09, newline 0x0A, carriage return 0x0D)
        for (char c : trimmed.toCharArray()) {
            if (c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) {
                throw new NodeValidationException("Node name cannot contain control characters");
            }
        }
        
        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            throw new NodeValidationException(
                "Node name contains invalid characters. Cannot use: / \\ : * ? \" < > |"
            );
        }
        
        if (trimmed.equals(".") || trimmed.equals("..")) {
            throw new NodeValidationException("Node name cannot be '.' or '..'");
        }
    }
    
    private static String normalizeWhitespace(String value) {
        return WHITESPACE_PATTERN.matcher(value).replaceAll(" ");
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeName nodeName = (NodeName) o;
        return Objects.equals(value, nodeName.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}

