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
class GetTreeUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private GetTreeUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new GetTreeUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should retrieve existing root node")
    void shouldRetrieveExistingRootNode() {
        // Given
        Node root = Node.createRoot();
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(root));
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(root);
        assertThat(result.isRoot()).isTrue();
        verify(nodeRepository).findRootNode();
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should create root node when none exists")
    void shouldCreateRootNodeWhenNoneExists() {
        // Given
        Node root = Node.createRoot();
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.empty());
        when(nodeRepository.save(any(Node.class))).thenReturn(root);
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isRoot()).isTrue();
        verify(nodeRepository).findRootNode();
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should retrieve node by ID successfully")
    void shouldRetrieveNodeByIdSuccessfully() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        NodeId nodeId = node.getId();
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
        
        // When
        Node result = useCase.getNode(nodeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(node);
        verify(nodeRepository).findById(nodeId);
    }
    
    @Test
    @DisplayName("Should fail when node ID not found")
    void shouldFailWhenNodeIdNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.getNode(nodeId))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
    }
    
    @Test
    @DisplayName("Should handle empty tree")
    void shouldHandleEmptyTree() {
        // Given
        Node root = Node.createRoot();
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(root));
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChildren()).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle large tree retrieval")
    void shouldHandleLargeTreeRetrieval() {
        // Given
        Node root = Node.createRoot();
        
        // Create a large tree structure
        for (int i = 0; i < 100; i++) {
            Node folder = Node.createFolder(NodeName.of("folder" + i), root);
            for (int j = 0; j < 10; j++) {
                Node.createFile(NodeName.of("file" + i + "_" + j + ".txt"), folder);
            }
        }
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(root));
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChildren()).hasSize(100);
        result.getChildren().forEach(folder -> {
            assertThat(folder.getChildren()).hasSize(10);
        });
    }
    
    @Test
    @DisplayName("Should handle deep tree retrieval")
    void shouldHandleDeepTreeRetrieval() {
        // Given
        Node current = Node.createRoot();
        
        // Create deep tree structure
        for (int i = 0; i < 50; i++) {
            current = Node.createFolder(NodeName.of("level" + i), current);
        }
        
        // Find root
        Node root = current;
        while (root.getParent().isPresent()) {
            root = root.getParent().get();
        }
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(root));
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isRoot()).isTrue();
    }
    
    @Test
    @DisplayName("Should preserve tree structure when retrieving")
    void shouldPreserveTreeStructureWhenRetrieving() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file1 = Node.createFile(NodeName.of("file1.txt"), folder1);
        Node file2 = Node.createFile(NodeName.of("file2.txt"), folder1);
        Node file3 = Node.createFile(NodeName.of("file3.txt"), folder2);
        
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(root));
        
        // When
        Node result = useCase.execute();
        
        // Then
        assertThat(result.getChildren()).contains(folder1, folder2);
        assertThat(folder1.getChildren()).contains(file1, file2);
        assertThat(folder2.getChildren()).contains(file3);
    }
    
    @Test
    @DisplayName("Should handle null node ID gracefully")
    void shouldHandleNullNodeIdGracefully() {
        // Given
        NodeId nullId = null;
        
        // When/Then
        assertThatThrownBy(() -> useCase.getNode(nullId))
            .isInstanceOf(Exception.class); // Will fail at repository level or earlier
    }
}

