package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class NodePathTest {
    
    @Test
    @DisplayName("Should enforce maximum depth limit")
    void shouldEnforceMaximumDepthLimit() {
        // Given - Create path at maximum depth
        NodePath path = NodePath.root();
        for (int i = 0; i < 50; i++) {
            path = path.append(NodeName.of("level" + i));
        }
        
        // Then - Should fail to exceed maximum depth
        final NodePath finalPath = path; // Make effectively final for lambda
        assertThatThrownBy(() -> finalPath.append(NodeName.of("exceed")))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("Maximum tree depth exceeded");
    }
    
    @Test
    @DisplayName("Should detect ancestor relationships correctly")
    void shouldDetectAncestorRelationships() {
        NodePath parent = NodePath.root().append(NodeName.of("parent"));
        NodePath child = parent.append(NodeName.of("child"));
        NodePath grandchild = child.append(NodeName.of("grandchild"));
        
        assertThat(parent.isAncestorOf(child)).isTrue();
        assertThat(parent.isAncestorOf(grandchild)).isTrue();
        assertThat(child.isAncestorOf(grandchild)).isTrue();
        assertThat(grandchild.isAncestorOf(parent)).isFalse();
    }
    
    @Test
    @DisplayName("Should prevent getParentPath on root")
    void shouldPreventGetParentPathOnRoot() {
        NodePath root = NodePath.root();
        assertThatThrownBy(() -> root.getParentPath())
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessage("Root node has no parent");
    }
    
    @Test
    @DisplayName("Should prevent path injection attempts")
    void shouldPreventPathInjectionAttempts() {
        // Given - Attempt to inject path separator or parent directory references
        NodePath base = NodePath.root().append(NodeName.of("folder"));
        
        // When/Then - Invalid names should be rejected at NodeName level
        assertThatThrownBy(() -> NodeName.of("../injected"))
            .isInstanceOf(NodeValidationException.class);
        
        assertThatThrownBy(() -> NodeName.of("../../etc/passwd"))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle paths with Unicode characters")
    void shouldHandlePathsWithUnicodeCharacters() {
        NodePath path = NodePath.root()
            .append(NodeName.of("测试"))
            .append(NodeName.of("文档"));
        
        assertThat(path.getDepth()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should handle malformed path edge cases")
    void shouldHandleMalformedPathEdgeCases() {
        // Path construction should always validate through NodeName
        // Invalid names are caught at NodeName creation
        assertThatThrownBy(() -> {
            NodeName invalid = NodeName.of("name/with/slash");
            NodePath.root().append(invalid);
        })
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle encoding edge cases")
    void shouldHandleEncodingEdgeCases() {
        // Path should handle various encodings correctly
        NodePath path1 = NodePath.root().append(NodeName.of("Café"));
        NodePath path2 = NodePath.root().append(NodeName.of("文件"));
        
        assertThat(path1.getDepth()).isEqualTo(1);
        assertThat(path2.getDepth()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should correctly identify self as ancestor")
    void shouldCorrectlyIdentifySelfAsAncestor() {
        NodePath path = NodePath.root().append(NodeName.of("folder"));
        
        // A path is not an ancestor of itself
        assertThat(path.isAncestorOf(path)).isFalse();
    }
    
    @Test
    @DisplayName("Should handle root path edge cases")
    void shouldHandleRootPathEdgeCases() {
        NodePath root = NodePath.root();
        
        assertThat(root.isRoot()).isTrue();
        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.isAncestorOf(root)).isFalse();
    }
}

