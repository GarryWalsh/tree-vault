package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class PositionTest {
    
    @Test
    @DisplayName("Should create valid position")
    void shouldCreateValidPosition() {
        Position position = Position.of(0);
        assertThat(position.getValue()).isEqualTo(0);
        
        Position positionMax = Position.of(10000);
        assertThat(positionMax.getValue()).isEqualTo(10000);
    }
    
    @Test
    @DisplayName("Should reject negative position")
    void shouldRejectNegativePosition() {
        assertThatThrownBy(() -> Position.of(-1))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot be negative");
    }
    
    @Test
    @DisplayName("Should reject position exceeding maximum")
    void shouldRejectPositionExceedingMaximum() {
        assertThatThrownBy(() -> Position.of(10001))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("exceeds maximum");
    }
    
    @Test
    @DisplayName("Should increment position within bounds")
    void shouldIncrementPosition() {
        Position pos = Position.of(0);
        Position incremented = pos.increment();
        assertThat(incremented.getValue()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should fail to increment at maximum position")
    void shouldFailToIncrementAtMaximum() {
        Position maxPos = Position.of(10000);
        assertThatThrownBy(() -> maxPos.increment())
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("increment position beyond maximum");
    }
    
    @Test
    @DisplayName("Should decrement position within bounds")
    void shouldDecrementPosition() {
        Position pos = Position.of(5);
        Position decremented = pos.decrement();
        assertThat(decremented.getValue()).isEqualTo(4);
    }
    
    @Test
    @DisplayName("Should fail to decrement at minimum position")
    void shouldFailToDecrementAtMinimum() {
        Position minPos = Position.of(0);
        assertThatThrownBy(() -> minPos.decrement())
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("decrement position below minimum");
    }
    
    @Test
    @DisplayName("Should handle integer boundary conditions")
    void shouldHandleIntegerBoundaryConditions() {
        // Test minimum valid position
        Position minPos = Position.of(0);
        assertThat(minPos.getValue()).isEqualTo(0);
        
        // Test maximum valid position
        Position maxPos = Position.of(10000);
        assertThat(maxPos.getValue()).isEqualTo(10000);
    }
    
    @Test
    @DisplayName("Should reject positions exceeding integer max")
    void shouldRejectPositionsExceedingIntegerMax() {
        // Position validation should catch values > 10000 before integer overflow
        assertThatThrownBy(() -> Position.of(Integer.MAX_VALUE))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("exceeds maximum");
    }
    
    @Test
    @DisplayName("Should handle increment at boundary")
    void shouldHandleIncrementAtBoundary() {
        Position pos9999 = Position.of(9999);
        Position pos10000 = pos9999.increment();
        
        assertThat(pos10000.getValue()).isEqualTo(10000);
        
        // Should fail to increment beyond max
        assertThatThrownBy(() -> pos10000.increment())
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle decrement at boundary")
    void shouldHandleDecrementAtBoundary() {
        Position pos1 = Position.of(1);
        Position pos0 = pos1.decrement();
        
        assertThat(pos0.getValue()).isEqualTo(0);
        
        // Should fail to decrement below min
        assertThatThrownBy(() -> pos0.decrement())
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should reject null position")
    void shouldRejectNullPosition() {
        // Position.of() should handle null input
        assertThatThrownBy(() -> {
            Integer nullValue = null;
            Position.of(nullValue);
        })
            .isInstanceOf(Exception.class);
    }
}

