package com.treevault.domain.service;

import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodePath;
import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathCalculationServiceTest {
    
    private PathCalculationService service;
    
    @BeforeEach
    void setUp() {
        service = new PathCalculationService();
    }
    
    @Test
    @DisplayName("Should calculate path for root child")
    void shouldCalculatePathForRootChild() {
        // Given
        NodePath parentPath = NodePath.root();
        NodeName nodeName = NodeName.of("Child");
        
        // When
        NodePath result = service.calculatePath(parentPath, nodeName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepth()).isEqualTo(1);
        assertThat(result.isRoot()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate path for nested child")
    void shouldCalculatePathForNestedChild() {
        // Given
        NodePath parentPath = NodePath.root().append(NodeName.of("Parent"));
        NodeName nodeName = NodeName.of("Child");
        
        // When
        NodePath result = service.calculatePath(parentPath, nodeName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepth()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should handle null parent path as root")
    void shouldHandleNullParentPathAsRoot() {
        // Given
        NodePath parentPath = null;
        NodeName nodeName = NodeName.of("Child");
        
        // When
        NodePath result = service.calculatePath(parentPath, nodeName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepth()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should calculate path for deeply nested structure")
    void shouldCalculatePathForDeeplyNestedStructure() {
        // Given - Build path up to max depth - 1
        NodePath currentPath = NodePath.root();
        for (int i = 0; i < 48; i++) {
            currentPath = currentPath.append(NodeName.of("level" + i));
        }
        NodeName nodeName = NodeName.of("FinalLevel");
        
        // When
        NodePath result = service.calculatePath(currentPath, nodeName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepth()).isEqualTo(49);
    }
    
    @Test
    @DisplayName("Should fail when exceeding maximum depth")
    void shouldFailWhenExceedingMaximumDepth() {
        // Given - Build path at max depth (root is 0, max depth is 49)
        NodePath currentPath = NodePath.root();
        for (int i = 0; i < 49; i++) {
            currentPath = currentPath.append(NodeName.of("level" + i));
        }
        final NodePath finalPath = currentPath; // Make effectively final for lambda
        NodeName nodeName = NodeName.of("Exceed");
        
        // When/Then - append() will throw when exceeding max depth
        assertThatThrownBy(() -> service.calculatePath(finalPath, nodeName))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("Maximum tree depth");
    }
    
    @Test
    @DisplayName("Should preserve path structure correctly")
    void shouldPreservePathStructureCorrectly() {
        // Given
        NodePath parentPath = NodePath.root()
            .append(NodeName.of("Level1"))
            .append(NodeName.of("Level2"));
        NodeName nodeName = NodeName.of("Level3");
        
        // When
        NodePath result = service.calculatePath(parentPath, nodeName);
        
        // Then
        assertThat(result.getDepth()).isEqualTo(3);
        assertThat(result.isAncestorOf(parentPath)).isFalse();
        assertThat(parentPath.isAncestorOf(result)).isTrue();
    }
}

