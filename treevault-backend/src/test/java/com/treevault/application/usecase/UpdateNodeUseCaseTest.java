package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.InvalidNodeOperationException;
import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.exception.NodeValidationException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateNodeUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private UpdateNodeUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new UpdateNodeUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should update node name successfully")
    void shouldUpdateNodeNameSuccessfully() {
        // Given
        Node node = Node.createFolder(NodeName.of("OldName"), null);
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            node.getId(),
            "NewName"
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Node result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName().getValue()).isEqualTo("NewName");
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when node not found")
    void shouldFailWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            nodeId,
            "NewName"
        );
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
    }
    
    @Test
    @DisplayName("Should fail when duplicate sibling name exists")
    void shouldFailWhenDuplicateSiblingNameExists() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Parent"), null);
        Node node1 = Node.createFolder(NodeName.of("Child1"), parent);
        Node node2 = Node.createFolder(NodeName.of("Child2"), parent);
        
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            node2.getId(),
            "Child1"
        );
        
        when(nodeRepository.findById(node2.getId())).thenReturn(Optional.of(node2));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    @DisplayName("Should fail when new name is invalid")
    void shouldFailWhenNewNameIsInvalid() {
        // Given
        Node node = Node.createFolder(NodeName.of("ValidName"), null);
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            node.getId(),
            ""  // Empty name
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should fail when new name contains invalid characters")
    void shouldFailWhenNewNameContainsInvalidCharacters() {
        // Given
        Node node = Node.createFolder(NodeName.of("ValidName"), null);
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            node.getId(),
            "invalid/name"
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("invalid characters");
    }
    
    @Test
    @DisplayName("Should update to same name without error")
    void shouldUpdateToSameNameWithoutError() {
        // Given
        Node node = Node.createFolder(NodeName.of("SameName"), null);
        UpdateNodeUseCase.UpdateNodeCommand command = new UpdateNodeUseCase.UpdateNodeCommand(
            node.getId(),
            "SameName"
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Node result = useCase.execute(command);
        
        // Then
        assertThat(result.getName().getValue()).isEqualTo("SameName");
        verify(nodeRepository).save(any(Node.class));
    }
}

