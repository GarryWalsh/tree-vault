package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import java.util.Objects;

public final class TagValue {
    private static final int MAX_LENGTH = 500;
    
    private final String value;
    
    private TagValue(String value) {
        this.value = value;
    }
    
    public static TagValue of(String value) {
        validateValue(value);
        return new TagValue(value);
    }
    
    private static void validateValue(String value) {
        if (value == null) {
            throw new NodeValidationException("Tag value cannot be null");
        }
        
        if (value.length() > MAX_LENGTH) {
            throw new NodeValidationException("Tag value cannot exceed " + MAX_LENGTH + " characters");
        }
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagValue tagValue = (TagValue) o;
        return Objects.equals(value, tagValue.value);
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

