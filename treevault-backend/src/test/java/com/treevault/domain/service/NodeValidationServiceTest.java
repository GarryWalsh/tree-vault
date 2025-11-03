package com.treevault.domain.service;

import com.treevault.BaseUnitTest;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.exception.CircularReferenceException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeValidationServiceTest extends BaseUnitTest {
    
    private NodeValidationService service;
    
    @BeforeEach
    void setUp() {
        service = new NodeValidationService();
    }
    
    @Test
    @DisplayName("Should validate node name successfully")
    void shouldValidateNodeNameSuccessfully() {
        // Given
        NodeName name = NodeName.of("ValidName");
        
        // When/Then - Should not throw exception
        service.validateNodeName(name);
    }
    
    @Test
    @DisplayName("Should fail when node name is invalid")
    void shouldFailWhenNodeNameIsInvalid() {
        // Given - Invalid name will fail at NodeName creation
        // When/Then
        assertThatThrownBy(() -> {
            NodeName invalidName = NodeName.of(""); // Will fail
            service.validateNodeName(invalidName);
        })
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should validate move operation successfully")
    void shouldValidateMoveOperationSuccessfully() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder1);
        
        // When/Then - Should not throw exception
        service.validateNodeForMove(file, folder2);
    }
    
    @Test
    @DisplayName("Should fail when moving node to itself")
    void shouldFailWhenMovingNodeToItself() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // When/Then
        assertThatThrownBy(() -> service.validateNodeForMove(node, node))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("Cannot move node to itself");
    }
    
    @Test
    @DisplayName("Should prevent circular reference when moving")
    void shouldPreventCircularReferenceWhenMoving() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Parent"), null);
        Node child = Node.createFolder(NodeName.of("Child"), parent);
        Node grandchild = Node.createFolder(NodeName.of("Grandchild"), child);
        
        // When/Then
        assertThatThrownBy(() -> service.validateNodeForMove(parent, grandchild))
            .isInstanceOf(CircularReferenceException.class)
            .hasMessageContaining("Cannot move node to its own descendant");
    }
    
    @Test
    @DisplayName("Should prevent moving node to direct descendant")
    void shouldPreventMovingNodeToDirectDescendant() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Parent"), null);
        Node child = Node.createFolder(NodeName.of("Child"), parent);
        
        // When/Then
        assertThatThrownBy(() -> service.validateNodeForMove(parent, child))
            .isInstanceOf(CircularReferenceException.class)
            .hasMessageContaining("Cannot move node to its own descendant");
    }
    
    @Test
    @DisplayName("Should allow moving node to sibling")
    void shouldAllowMovingNodeToSibling() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder1);
        
        // When/Then - Should not throw exception
        service.validateNodeForMove(file, folder2);
    }
    
    @Test
    @DisplayName("Should allow moving node up the tree")
    void shouldAllowMovingNodeUpTheTree() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), folder1);
        Node file = Node.createFile(NodeName.of("file.txt"), folder2);
        
        // When/Then - Should not throw exception (moving up is allowed)
        service.validateNodeForMove(file, folder1);
    }
    
    @Test
    @DisplayName("Should fail validation with null node")
    void shouldFailValidationWithNullNode() {
        // Given
        Node targetParent = Node.createFolder(NodeName.of("Parent"), null);
        
        // When/Then
        assertThatThrownBy(() -> service.validateNodeForMove(null, targetParent))
            .isInstanceOf(NullPointerException.class);
    }
    
    @Test
    @DisplayName("Should fail validation with null target parent")
    void shouldFailValidationWithNullTargetParent() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // When/Then
        assertThatThrownBy(() -> service.validateNodeForMove(node, null))
            .isInstanceOf(NullPointerException.class);
    }
}

