package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import java.util.Objects;

public final class Position {
    private static final int MIN_POSITION = 0;
    private static final int MAX_POSITION = 10000;
    
    private final int value;
    
    private Position(int value) {
        this.value = value;
    }
    
    public static Position of(int value) {
        validatePosition(value);
        return new Position(value);
    }
    
    private static void validatePosition(int value) {
        if (value < MIN_POSITION) {
            throw new NodeValidationException(
                String.format("Position cannot be negative: %d (minimum: %d)", value, MIN_POSITION)
            );
        }
        
        if (value > MAX_POSITION) {
            throw new NodeValidationException(
                String.format("Position exceeds maximum: %d (maximum: %d)", value, MAX_POSITION)
            );
        }
    }
    
    public int getValue() {
        return value;
    }
    
    public Position increment() {
        if (value >= MAX_POSITION) {
            throw new NodeValidationException("Cannot increment position beyond maximum");
        }
        return new Position(value + 1);
    }
    
    public Position decrement() {
        if (value <= MIN_POSITION) {
            throw new NodeValidationException("Cannot decrement position below minimum");
        }
        return new Position(value - 1);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return value == position.value;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

