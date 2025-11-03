package com.treevault.application.usecase;

import com.treevault.BaseUnitTest;
import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetTreeUseCaseTest extends BaseUnitTest {

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private GetTreeUseCase getTreeUseCase;

    @Test
    @DisplayName("Should return existing root when it exists")
    void shouldReturnExistingRoot() {
        // Given
        Node existingRoot = Node.createRoot();
        when(nodeRepository.findRootNode()).thenReturn(Optional.of(existingRoot));

        // When
        Node result = getTreeUseCase.execute();

        // Then
        assertThat(result).isEqualTo(existingRoot);
        verify(nodeRepository).findRootNode();
        verify(nodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create and save new root when it doesn't exist")
    void shouldCreateAndSaveNewRootWhenNotExists() {
        // Given
        when(nodeRepository.findRootNode()).thenReturn(Optional.empty());
        
        // Mock the save to return a root node
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> {
            Node savedNode = invocation.getArgument(0);
            return savedNode;
        });

        // When
        Node result = getTreeUseCase.execute();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName().getValue()).isEqualTo("root");
        assertThat(result.getParent()).isEmpty();
        
        // CRITICAL: Verify that save was called (transaction must commit this)
        verify(nodeRepository).findRootNode();
        verify(nodeRepository).save(any(Node.class));
    }

    @Test
    @DisplayName("Should call save exactly once when root doesn't exist")
    void shouldCallSaveExactlyOnceForNewRoot() {
        // Given
        when(nodeRepository.findRootNode()).thenReturn(Optional.empty());
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        getTreeUseCase.execute();

        // Then - Save should be called exactly once, not multiple times
        verify(nodeRepository, times(1)).save(any(Node.class));
    }

    @Test
    @DisplayName("Should not create duplicate roots on multiple calls")
    void shouldNotCreateDuplicateRootsOnMultipleCalls() {
        // Given
        Node createdRoot = Node.createRoot();
        when(nodeRepository.findRootNode())
            .thenReturn(Optional.empty())  // First call: no root
            .thenReturn(Optional.of(createdRoot));  // Second call: root exists
        when(nodeRepository.save(any(Node.class))).thenReturn(createdRoot);

        // When
        Node firstResult = getTreeUseCase.execute();
        Node secondResult = getTreeUseCase.execute();

        // Then
        assertThat(firstResult).isNotNull();
        assertThat(secondResult).isNotNull();
        verify(nodeRepository, times(2)).findRootNode();
        verify(nodeRepository, times(1)).save(any(Node.class));  // Save only called once
    }

    @Test
    @DisplayName("Should return node by ID when it exists")
    void shouldReturnNodeById() {
        // Given
        NodeId nodeId = NodeId.of(UUID.randomUUID());
        Node node = Node.createRoot();
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(node));

        // When
        Node result = getTreeUseCase.getNode(nodeId);

        // Then
        assertThat(result).isEqualTo(node);
        verify(nodeRepository).findById(nodeId);
    }

    @Test
    @DisplayName("Should throw NodeNotFoundException when node doesn't exist")
    void shouldThrowExceptionWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.of(UUID.randomUUID());
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> getTreeUseCase.getNode(nodeId))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found: " + nodeId);
        
        verify(nodeRepository).findById(nodeId);
    }

    @Test
    @DisplayName("Should handle null safely in root creation scenario")
    void shouldHandleNullSafely() {
        // Given
        when(nodeRepository.findRootNode()).thenReturn(Optional.empty());
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Node result = getTreeUseCase.execute();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isNotNull();
        assertThat(result.getType()).isNotNull();
        assertThat(result.getId()).isNotNull();
    }
}
