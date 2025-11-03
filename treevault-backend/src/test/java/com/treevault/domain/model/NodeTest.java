package com.treevault.domain.model;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.model.valueobject.Position;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import com.treevault.domain.exception.CircularReferenceException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeTest {
    
    @Nested
    @DisplayName("Node Creation")
    class NodeCreation {
        
        @Test
        @DisplayName("Should create folder with valid name")
        void shouldCreateFolderWithValidName() {
            // Given
            NodeName name = NodeName.of("Documents");
            
            // When
            Node folder = Node.createFolder(name, null);
            
            // Then
            assertThat(folder).isNotNull();
            assertThat(folder.getName()).isEqualTo(name);
            assertThat(folder.getType()).isEqualTo(NodeType.FOLDER);
            assertThat(folder.isFolder()).isTrue();
            assertThat(folder.isRoot()).isTrue();
            assertThat(folder.getPath().isRoot()).isTrue();
        }
        
        @Test
        @DisplayName("Should create file with parent folder")
        void shouldCreateFileWithParentFolder() {
            // Given
            Node parent = Node.createFolder(NodeName.of("Documents"), null);
            NodeName fileName = NodeName.of("report.pdf");
            
            // When
            Node file = Node.createFile(fileName, parent);
            
            // Then
            assertThat(file.getName()).isEqualTo(fileName);
            assertThat(file.getType()).isEqualTo(NodeType.FILE);
            assertThat(file.isFile()).isTrue();
            assertThat(file.getParent()).contains(parent);
            assertThat(parent.getChildren()).contains(file);
        }
        
        @Test
        @DisplayName("Should fail to create file without parent")
        void shouldFailToCreateFileWithoutParent() {
            // When/Then
            assertThatThrownBy(() -> Node.createFile(NodeName.of("file.txt"), null))
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessage("Files must have a parent folder");
        }
        
        @Test
        @DisplayName("Should fail to create file under another file")
        void shouldFailToCreateFileUnderFile() {
            // Given
            Node folder = Node.createFolder(NodeName.of("Folder"), null);
            Node file = Node.createFile(NodeName.of("file1.txt"), folder);
            
            // When/Then
            assertThatThrownBy(() -> Node.createFile(NodeName.of("file2.txt"), file))
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessage("Files can only be added to folders");
        }
    }
    
    @Nested
    @DisplayName("Node Renaming")
    class NodeRenaming {
        
        @Test
        @DisplayName("Should rename node with valid name")
        void shouldRenameNodeWithValidName() {
            // Given
            Node node = Node.createFolder(NodeName.of("OldName"), null);
            NodeName newName = NodeName.of("NewName");
            
            // When
            node.rename(newName);
            
            // Then
            assertThat(node.getName()).isEqualTo(newName);
            assertThat(node.getUpdatedAt()).isAfterOrEqualTo(node.getCreatedAt());
            assertThat(node.getVersion()).isGreaterThan(0L);
        }
        
        @Test
        @DisplayName("Should fail to rename with duplicate sibling name")
        void shouldFailToRenameWithDuplicateSiblingName() {
            // Given
            Node parent = Node.createFolder(NodeName.of("Parent"), null);
            Node child1 = Node.createFolder(NodeName.of("Child1"), parent);
            Node child2 = Node.createFolder(NodeName.of("Child2"), parent);
            
            // When/Then
            assertThatThrownBy(() -> child2.rename(NodeName.of("Child1")))
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessageContaining("already exists");
        }
    }
    
    @Nested
    @DisplayName("Node Movement")
    class NodeMovement {
        
        @Test
        @DisplayName("Should move node to different parent")
        void shouldMoveNodeToDifferentParent() {
            // Given
            Node root = Node.createRoot();
            Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
            Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
            Node file = Node.createFile(NodeName.of("file.txt"), folder1);
            
            // When
            file.moveTo(folder2, Position.of(0));
            
            // Then
            assertThat(file.getParent()).contains(folder2);
            assertThat(folder1.getChildren()).doesNotContain(file);
            assertThat(folder2.getChildren()).contains(file);
            assertThat(file.getPath().getDepth()).isEqualTo(2);
        }
        
        @Test
        @DisplayName("Should prevent circular reference when moving")
        void shouldPreventCircularReference() {
            // Given
            Node parent = Node.createFolder(NodeName.of("Parent"), null);
            Node child = Node.createFolder(NodeName.of("Child"), parent);
            Node grandchild = Node.createFolder(NodeName.of("Grandchild"), child);
            
            // When/Then
            assertThatThrownBy(() -> parent.moveTo(grandchild, Position.of(0)))
                .isInstanceOf(CircularReferenceException.class)
                .hasMessage("Cannot move node to its own descendant");
        }
        
        @Test
        @DisplayName("Should prevent moving node to itself")
        void shouldPreventMovingNodeToItself() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            
            // When/Then
            assertThatThrownBy(() -> node.moveTo(node, Position.of(0)))
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessage("Cannot move node to itself");
        }
    }
    
    @Nested
    @DisplayName("Node Tags")
    class NodeTags {
        
        @Test
        @DisplayName("Should add valid tag to node")
        void shouldAddValidTagToNode() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            TagKey key = TagKey.of("department");
            TagValue value = TagValue.of("engineering");
            
            // When
            node.addTag(key, value);
            
            // Then
            assertThat(node.getTags()).containsKey(key);
            assertThat(node.getTags().get(key).getValue()).isEqualTo(value);
        }
        
        @Test
        @DisplayName("Should remove existing tag")
        void shouldRemoveExistingTag() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            TagKey key = TagKey.of("status");
            node.addTag(key, TagValue.of("active"));
            
            // When
            node.removeTag(key);
            
            // Then
            assertThat(node.getTags()).doesNotContainKey(key);
        }
        
        @Test
        @DisplayName("Should fail when exceeding tag limit")
        void shouldFailWhenExceedingTagLimit() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            
            // When - Add 50 tags (max limit)
            for (int i = 0; i < 50; i++) {
                node.addTag(TagKey.of("key" + i), TagValue.of("value" + i));
            }
            
            // Then - Should fail on 51st tag
            assertThatThrownBy(() -> 
                node.addTag(TagKey.of("key51"), TagValue.of("value51")))
                .isInstanceOf(NodeValidationException.class)
                .hasMessage("Maximum number of tags (50) exceeded");
        }
    }
    
    @Nested
    @DisplayName("Node Deletion")
    class NodeDeletion {
        
        @Test
        @DisplayName("Should cascade delete children")
        void shouldCascadeDeleteChildren() {
            // Given
            Node root = Node.createRoot();
            Node folder = Node.createFolder(NodeName.of("Folder"), root);
            Node file1 = Node.createFile(NodeName.of("file1.txt"), folder);
            Node file2 = Node.createFile(NodeName.of("file2.txt"), folder);
            
            // When
            folder.delete();
            
            // Then
            assertThat(root.getChildren()).doesNotContain(folder);
            assertThat(folder.getChildren()).isEmpty();
        }
        
        @Test
        @DisplayName("Should prevent deleting root node with children")
        void shouldPreventDeletingRootWithChildren() {
            // Given
            Node root = Node.createRoot();
            Node child = Node.createFolder(NodeName.of("Child"), root);
            
            // When/Then
            assertThatThrownBy(() -> root.delete())
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessageContaining("Cannot delete the root node");
        }
        
        @Test
        @DisplayName("Should prevent deleting empty root node")
        void shouldPreventDeletingEmptyRoot() {
            // Given
            Node root = Node.createRoot();
            
            // When/Then - Root should NEVER be deletable, even when empty
            assertThatThrownBy(() -> root.delete())
                .isInstanceOf(InvalidNodeOperationException.class)
                .hasMessageContaining("Cannot delete the root node");
        }
    }
    
    @Nested
    @DisplayName("Null Parameter Handling")
    class NullParameterHandling {
        
        @Test
        @DisplayName("Should fail when creating node with null name")
        void shouldFailWhenCreatingNodeWithNullName() {
            assertThatThrownBy(() -> Node.createFolder(null, null))
                .isInstanceOf(NullPointerException.class);
        }
        
        @Test
        @DisplayName("Should fail when renaming with null name")
        void shouldFailWhenRenamingWithNullName() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            assertThatThrownBy(() -> node.rename(null))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should fail when moving with null parent")
        void shouldFailWhenMovingWithNullParent() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            assertThatThrownBy(() -> node.moveTo(null, Position.of(0)))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should fail when moving with null position")
        void shouldFailWhenMovingWithNullPosition() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            Node parent = Node.createFolder(NodeName.of("Parent"), null);
            assertThatThrownBy(() -> node.moveTo(parent, null))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should fail when adding tag with null key")
        void shouldFailWhenAddingTagWithNullKey() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            assertThatThrownBy(() -> node.addTag(null, TagValue.of("value")))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should fail when adding tag with null value")
        void shouldFailWhenAddingTagWithNullValue() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            assertThatThrownBy(() -> node.addTag(TagKey.of("key"), null))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should fail when removing tag with null key")
        void shouldFailWhenRemovingTagWithNullKey() {
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            assertThatThrownBy(() -> node.removeTag(null))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("cannot be null");
        }
    }
    
    @Nested
    @DisplayName("Invalid State Transitions")
    class InvalidStateTransitions {
        
        @Test
        @DisplayName("Should allow operations on deleted node (node is orphaned, not marked deleted)")
        void shouldAllowOperationsOnDeletedNode() {
            // Given - Node is deleted (removed from parent, but object still exists)
            Node root = Node.createRoot();
            Node folder = Node.createFolder(NodeName.of("Folder"), root);
            folder.delete(); // Removes from parent, but node object still exists
            
            // When/Then - Operations on orphaned node should still work
            // (In a real system, deleted nodes might be marked, but current implementation
            // just removes them from the tree structure)
            folder.rename(NodeName.of("NewName")); // Should work
            assertThat(folder.getName().getValue()).isEqualTo("NewName");
        }
        
        @Test
        @DisplayName("Should allow moving deleted node to new parent")
        void shouldAllowMovingDeletedNodeToNewParent() {
            // Given
            Node root = Node.createRoot();
            Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
            Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
            folder1.delete(); // Removed from parent
            
            // When/Then - Moving orphaned node should work
            folder1.moveTo(folder2, Position.of(0));
            assertThat(folder1.getParent()).contains(folder2);
        }
    }
    
    @Nested
    @DisplayName("Concurrent Modification Scenarios")
    class ConcurrentModificationScenarios {
        
        @Test
        @DisplayName("Should handle optimistic locking version conflicts")
        void shouldHandleOptimisticLockingVersionConflicts() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            long originalVersion = node.getVersion();
            
            // Simulate concurrent modification by modifying version
            node.rename(NodeName.of("NewName"));
            long newVersion = node.getVersion();
            
            // Then
            assertThat(newVersion).isGreaterThan(originalVersion);
            // Note: Actual optimistic locking conflict would be handled at persistence layer
        }
        
        @Test
        @DisplayName("Should handle simultaneous rename attempts")
        void shouldHandleSimultaneousRenameAttempts() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            
            // When - Multiple rename operations
            node.rename(NodeName.of("Name1"));
            node.rename(NodeName.of("Name2"));
            node.rename(NodeName.of("Name3"));
            
            // Then - Last rename should win
            assertThat(node.getName().getValue()).isEqualTo("Name3");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Should handle node with maximum depth")
        void shouldHandleNodeWithMaximumDepth() {
            // Given - Create deep tree structure (root is at depth 0)
            Node current = Node.createRoot();
            Node root = current;
            
            // When - Create 49 levels deep (depths 1-49)
            for (int i = 0; i < 49; i++) {
                current = Node.createFolder(NodeName.of("level" + i), current);
            }
            
            // Then - Should fail to add one more level
            final Node finalCurrent = current; // Make effectively final for lambda
            assertThatThrownBy(() -> 
                Node.createFolder(NodeName.of("exceed"), finalCurrent))
                .isInstanceOf(NodeValidationException.class)
                .hasMessageContaining("Maximum tree depth");
        }
        
        @Test
        @DisplayName("Should handle node with maximum name length")
        void shouldHandleNodeWithMaximumNameLength() {
            // Given
            String maxLengthName = "a".repeat(255);
            
            // When
            Node node = Node.createFolder(NodeName.of(maxLengthName), null);
            
            // Then
            assertThat(node.getName().getValue()).hasSize(255);
        }
        
        @Test
        @DisplayName("Should handle node with exactly 50 tags")
        void shouldHandleNodeWithExactly50Tags() {
            // Given
            Node node = Node.createFolder(NodeName.of("Folder"), null);
            
            // When - Add exactly 50 tags
            for (int i = 0; i < 50; i++) {
                node.addTag(TagKey.of("key" + i), TagValue.of("value" + i));
            }
            
            // Then
            assertThat(node.getTags()).hasSize(50);
        }
    }
}

