package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import com.treevault.domain.repository.NodeRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveTagUseCaseTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private RemoveTagUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new RemoveTagUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should remove tag from node successfully")
    void shouldRemoveTagFromNodeSuccessfully() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        node.addTag(key, value);
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        useCase.execute(node.getId(), key);
        
        // Then
        assertThat(node.getTags()).doesNotContainKey(key);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when node not found")
    void shouldFailWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        TagKey key = TagKey.of("department");
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(nodeId, key))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when tag not found")
    void shouldFailWhenTagNotFound() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey existingKey = TagKey.of("existing");
        TagKey missingKey = TagKey.of("missing");
        node.addTag(existingKey, TagValue.of("value"));
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(node.getId(), missingKey))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Tag with key");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should handle removing tag from node with multiple tags")
    void shouldHandleRemovingTagFromNodeWithMultipleTags() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key1 = TagKey.of("department");
        TagKey key2 = TagKey.of("status");
        TagKey key3 = TagKey.of("priority");
        
        node.addTag(key1, TagValue.of("engineering"));
        node.addTag(key2, TagValue.of("active"));
        node.addTag(key3, TagValue.of("high"));
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        useCase.execute(node.getId(), key2);
        
        // Then
        assertThat(node.getTags()).doesNotContainKey(key2);
        assertThat(node.getTags()).containsKey(key1);
        assertThat(node.getTags()).containsKey(key3);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should handle case-insensitive tag key removal")
    void shouldHandleCaseInsensitiveTagKeyRemoval() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("DEPARTMENT"); // Will be normalized to lowercase
        node.addTag(key, TagValue.of("engineering"));
        
        TagKey removeKey = TagKey.of("department"); // Lowercase
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        useCase.execute(node.getId(), removeKey);
        
        // Then
        assertThat(node.getTags()).doesNotContainKey(key);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when tag key is invalid")
    void shouldFailWhenTagKeyIsInvalid() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // When/Then - Invalid tag key creation should fail before execution
        assertThatThrownBy(() -> {
            TagKey invalidKey = TagKey.of(""); // Empty key
            useCase.execute(node.getId(), invalidKey);
        })
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should remove last tag from node")
    void shouldRemoveLastTagFromNode() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("only");
        node.addTag(key, TagValue.of("value"));
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        useCase.execute(node.getId(), key);
        
        // Then
        assertThat(node.getTags()).isEmpty();
        verify(nodeRepository).save(any(Node.class));
    }
}

