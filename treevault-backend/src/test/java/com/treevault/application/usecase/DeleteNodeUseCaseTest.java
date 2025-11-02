package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteNodeUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private DeleteNodeUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new DeleteNodeUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should delete node successfully")
    void shouldDeleteNodeSuccessfully() {
        // Given
        Node root = Node.createRoot();
        Node node = Node.createFolder(NodeName.of("ToDelete"), root);
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        doNothing().when(nodeRepository).delete(any(Node.class));
        
        // When
        useCase.execute(node.getId());
        
        // Then
        verify(nodeRepository).findById(node.getId());
        verify(nodeRepository).delete(node);
    }
    
    @Test
    @DisplayName("Should cascade delete children")
    void shouldCascadeDeleteChildren() {
        // Given
        Node root = Node.createRoot();
        Node folder = Node.createFolder(NodeName.of("Folder"), root);
        Node file1 = Node.createFile(NodeName.of("file1.txt"), folder);
        Node file2 = Node.createFile(NodeName.of("file2.txt"), folder);
        
        when(nodeRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        doNothing().when(nodeRepository).delete(any(Node.class));
        
        // When
        useCase.execute(folder.getId());
        
        // Then
        assertThat(folder.getChildren()).isEmpty();
        verify(nodeRepository).delete(folder);
    }
    
    @Test
    @DisplayName("Should fail when node not found")
    void shouldFailWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(nodeId))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
        
        verify(nodeRepository, never()).delete(any(Node.class));
    }
    
    @Test
    @DisplayName("Should prevent deleting root with children")
    void shouldPreventDeletingRootWithChildren() {
        // Given
        Node root = Node.createRoot();
        Node child = Node.createFolder(NodeName.of("Child"), root);
        
        when(nodeRepository.findById(root.getId())).thenReturn(Optional.of(root));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(root.getId()))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("Cannot delete the root node");
        
        verify(nodeRepository, never()).delete(any(Node.class));
    }
    
    @Test
    @DisplayName("Should prevent deleting root without children")
    void shouldPreventDeletingRootWithoutChildren() {
        // Given
        Node root = Node.createRoot();
        
        when(nodeRepository.findById(root.getId())).thenReturn(Optional.of(root));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(root.getId()))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("Cannot delete the root node");
        
        verify(nodeRepository, never()).delete(any(Node.class));
    }
    
    @Test
    @DisplayName("Should handle deep nested deletion")
    void shouldHandleDeepNestedDeletion() {
        // Given - Create a deep tree structure
        Node root = Node.createRoot();
        Node level1 = Node.createFolder(NodeName.of("Level1"), root);
        Node level2 = Node.createFolder(NodeName.of("Level2"), level1);
        Node level3 = Node.createFolder(NodeName.of("Level3"), level2);
        
        when(nodeRepository.findById(level1.getId())).thenReturn(Optional.of(level1));
        doNothing().when(nodeRepository).delete(any(Node.class));
        
        // When
        useCase.execute(level1.getId());
        
        // Then
        assertThat(level1.getChildren()).isEmpty();
        verify(nodeRepository).delete(level1);
    }
}

