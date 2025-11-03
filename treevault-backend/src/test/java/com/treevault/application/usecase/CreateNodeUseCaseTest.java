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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateNodeUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private CreateNodeUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new CreateNodeUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should create folder node successfully")
    void shouldCreateFolderNodeSuccessfully() {
        // Given
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "Documents",
            NodeType.FOLDER,
            null,
            null
        );
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.empty());
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Node result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName().getValue()).isEqualTo("Documents");
        assertThat(result.getType()).isEqualTo(NodeType.FOLDER);
        verify(nodeRepository, times(2)).save(any(Node.class));
        verify(nodeRepository).existsByParentAndName(any(NodeId.class), eq(NodeName.of("Documents")));
    }
    
    @Test
    @DisplayName("Should create file node with parent successfully")
    void shouldCreateFileNodeWithParentSuccessfully() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Documents"), null);
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "report.pdf",
            NodeType.FILE,
            parent.getId(),
            null
        );
        
        when(nodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(nodeRepository.existsByParentAndName(any(NodeId.class), any(NodeName.class))).thenReturn(false);
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Node result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName().getValue()).isEqualTo("report.pdf");
        assertThat(result.getType()).isEqualTo(NodeType.FILE);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when parent not found")
    void shouldFailWhenParentNotFound() {
        // Given
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "report.pdf",
            NodeType.FILE,
            NodeId.generate(),
            null
        );
        
        when(nodeRepository.findById(any())).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Parent node not found");
    }
    
    @Test
    @DisplayName("Should fail when duplicate name exists")
    void shouldFailWhenDuplicateNameExists() {
        // Given
        Node parent = Node.createFolder(NodeName.of("Documents"), null);
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "report.pdf",
            NodeType.FILE,
            parent.getId(),
            null
        );
        
        when(nodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(nodeRepository.existsByParentAndName(any(NodeId.class), any(NodeName.class))).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    @DisplayName("Should fail when repository save throws exception")
    void shouldFailWhenRepositorySaveThrowsException() {
        // Given
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "Documents",
            NodeType.FOLDER,
            null,
            null
        );
        
        when(nodeRepository.save(any(Node.class))).thenThrow(new RuntimeException("Database connection failed"));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
        
        // When parent is null (root level), duplicate check is not performed
        verify(nodeRepository, never()).existsByParentAndName(any(), any());
    }
    
    @Test
    @DisplayName("Should fail when repository findById throws exception")
    void shouldFailWhenRepositoryFindByIdThrowsException() {
        // Given
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "report.pdf",
            NodeType.FILE,
            NodeId.generate(),
            null
        );
        
        when(nodeRepository.findById(any())).thenThrow(new RuntimeException("Database error"));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database error");
    }
    
    @Test
    @DisplayName("Should fail when checking duplicate name throws exception")
    void shouldFailWhenCheckingDuplicateNameThrowsException() {
        // Given - Create a node with a parent to trigger duplicate check
        Node parent = Node.createFolder(NodeName.of("Parent"), null);
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "Documents",
            NodeType.FOLDER,
            parent.getId(),
            null
        );
        
        when(nodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(nodeRepository.existsByParentAndName(any(NodeId.class), any(NodeName.class)))
            .thenThrow(new RuntimeException("Connection timeout"));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Connection timeout");
    }
    
    @Test
    @DisplayName("Should fail when parent is not a folder")
    void shouldFailWhenParentIsNotAFolder() {
        // Given
        Node parent = Node.createFile(NodeName.of("file.txt"), Node.createFolder(NodeName.of("folder"), null));
        CreateNodeUseCase.CreateNodeCommand command = new CreateNodeUseCase.CreateNodeCommand(
            "report.pdf",
            NodeType.FILE,
            parent.getId(),
            null
        );
        
        when(nodeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        
        // When/Then - This should fail because files can't have children
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(InvalidNodeOperationException.class);
    }
}

