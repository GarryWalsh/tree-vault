package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveNodeUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private MoveNodeUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new MoveNodeUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should move node to different parent successfully")
    void shouldMoveNodeToDifferentParentSuccessfully() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder1);
        
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            file.getId(),
            folder2.getId(),
            Position.of(0)
        );
        
        when(nodeRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(nodeRepository.findById(folder2.getId())).thenReturn(Optional.of(folder2));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Node result = useCase.execute(command);
        
        // Then
        assertThat(result.getParent()).contains(folder2);
        assertThat(folder1.getChildren()).doesNotContain(file);
        assertThat(folder2.getChildren()).contains(file);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when node not found")
    void shouldFailWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        NodeId parentId = NodeId.generate();
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            nodeId,
            parentId,
            Position.of(0)
        );
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
    }
    
    @Test
    @DisplayName("Should fail when parent not found")
    void shouldFailWhenParentNotFound() {
        // Given
        Node node = Node.createFolder(NodeName.of("Node"), null);
        NodeId parentId = NodeId.generate();
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            node.getId(),
            parentId,
            Position.of(0)
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.findById(parentId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Parent node not found");
    }
    
    @Test
    @DisplayName("Should prevent circular reference when moving")
    void shouldPreventCircularReferenceWhenMoving() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Parent"), null);
        Node child = Node.createFolder(NodeName.of("Child"), parent);
        Node grandchild = Node.createFolder(NodeName.of("Grandchild"), child);
        
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            parent.getId(),
            grandchild.getId(),
            Position.of(0)
        );
        
        when(nodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(nodeRepository.findById(grandchild.getId())).thenReturn(Optional.of(grandchild));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(CircularReferenceException.class)
            .hasMessageContaining("Cannot move node to its own descendant");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should prevent moving node to itself")
    void shouldPreventMovingNodeToItself() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            node.getId(),
            node.getId(),
            Position.of(0)
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("Cannot move node to itself");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when name conflict exists in target folder")
    void shouldFailWhenNameConflictExistsInTargetFolder() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file1 = Node.createFile(NodeName.of("file.txt"), folder1);
        Node file2 = Node.createFile(NodeName.of("file.txt"), folder2);
        
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            file1.getId(),
            folder2.getId(),
            Position.of(0)
        );
        
        when(nodeRepository.findById(file1.getId())).thenReturn(Optional.of(file1));
        when(nodeRepository.findById(folder2.getId())).thenReturn(Optional.of(folder2));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("already exists");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when moving file to another file")
    void shouldFailWhenMovingFileToAnotherFile() {
        // Given
        Node root = Node.createRoot();
        Node folder = Node.createFolder(NodeName.of("Folder"), root);
        Node file1 = Node.createFile(NodeName.of("file1.txt"), folder);
        Node file2 = Node.createFile(NodeName.of("file2.txt"), folder);
        
        MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
            file1.getId(),
            file2.getId(),
            Position.of(0)
        );
        
        when(nodeRepository.findById(file1.getId())).thenReturn(Optional.of(file1));
        when(nodeRepository.findById(file2.getId())).thenReturn(Optional.of(file2));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("can only be moved to folders");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should validate position bounds")
    void shouldValidatePositionBounds() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder1);
        
        // When/Then - Position validation happens when creating Position object
        assertThatThrownBy(() -> {
            Position invalidPosition = Position.of(10001); // This will throw
            MoveNodeUseCase.MoveNodeCommand command = new MoveNodeUseCase.MoveNodeCommand(
                file.getId(),
                folder2.getId(),
                invalidPosition
            );
            useCase.execute(command);
        })
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("Position exceeds maximum");
    }
}

