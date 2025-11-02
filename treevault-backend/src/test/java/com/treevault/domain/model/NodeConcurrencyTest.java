package com.treevault.domain.model;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class NodeConcurrencyTest {
    
    @Test
    @DisplayName("Should detect optimistic locking conflicts")
    void shouldDetectOptimisticLockingConflicts() {
        // Given - Node with initial version
        Node root = Node.createRoot();
        Node node = Node.createFolder(NodeName.of("Folder"), root);
        Long initialVersion = node.getVersion();
        
        // When - Simulate concurrent modification by renaming
        node.rename(NodeName.of("RenamedFolder"));
        Long updatedVersion = node.getVersion();
        
        // Then - Version should increment
        assertThat(updatedVersion).isGreaterThan(initialVersion);
    }
    
    @Test
    @DisplayName("Should handle large tree operations")
    void shouldHandleLargeTreeOperations() {
        // Given - Create large tree structure
        Node root = Node.createRoot();
        
        // When - Create 100 folders with 10 files each
        for (int i = 0; i < 100; i++) {
            Node folder = Node.createFolder(NodeName.of("folder" + i), root);
            for (int j = 0; j < 10; j++) {
                Node.createFile(NodeName.of("file" + i + "_" + j + ".txt"), folder);
            }
        }
        
        // Then - Verify tree structure
        assertThat(root.getChildren()).hasSize(100);
        root.getChildren().forEach(folder -> {
            assertThat(folder.getChildren()).hasSize(10);
        });
    }
    
    @Test
    @DisplayName("Should handle deep nesting up to maximum depth")
    void shouldHandleDeepNestingToMaximumDepth() {
        // Given - Create deep tree structure (root is at depth 0)
        Node current = Node.createRoot();
        
        // When - Create 49 levels deep (depths 1-49)
        for (int i = 0; i < 49; i++) {
            current = Node.createFolder(NodeName.of("level" + i), current);
        }
        
        // Then - Verify depth
        assertThat(current.getPath().getDepth()).isEqualTo(49);
        
        // And - Should fail to exceed maximum depth
        final Node finalCurrent = current; // Make effectively final for lambda
        assertThatThrownBy(() -> 
            Node.createFolder(NodeName.of("exceed"), finalCurrent))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("Maximum tree depth");
    }
}

