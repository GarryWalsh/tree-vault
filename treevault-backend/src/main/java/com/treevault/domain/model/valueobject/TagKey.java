package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TagKey {
    private static final int MAX_LENGTH = 100;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.-]*$");
    
    private final String value;
    
    private TagKey(String value) {
        this.value = value;
    }
    
    public static TagKey of(String value) {
        validateKey(value);
        return new TagKey(value.toLowerCase());
    }
    
    private static void validateKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new NodeValidationException("Tag key cannot be null or empty");
        }
        
        if (value.length() > MAX_LENGTH) {
            throw new NodeValidationException("Tag key cannot exceed " + MAX_LENGTH + " characters");
        }
        
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new NodeValidationException(
                "Tag key must start with a letter and contain only letters, numbers, dots, hyphens, and underscores"
            );
        }
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagKey tagKey = (TagKey) o;
        return Objects.equals(value, tagKey.value);
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

