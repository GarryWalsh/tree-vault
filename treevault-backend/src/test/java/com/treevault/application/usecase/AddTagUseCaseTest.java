package com.treevault.application.usecase;

import com.treevault.BaseUnitTest;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddTagUseCaseTest extends BaseUnitTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private AddTagUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new AddTagUseCase(nodeRepository);
    }
    
    @Test
    @DisplayName("Should add tag to node successfully")
    void shouldAddTagToNodeSuccessfully() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        
        AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
            node.getId(),
            key,
            value
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Tag result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getValue()).isEqualTo(value);
        assertThat(node.getTags()).containsKey(key);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when node not found")
    void shouldFailWhenNodeNotFound() {
        // Given
        NodeId nodeId = NodeId.generate();
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        
        AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
            nodeId,
            key,
            value
        );
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeNotFoundException.class)
            .hasMessageContaining("Node not found");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when tag limit exceeded")
    void shouldFailWhenTagLimitExceeded() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // Add 50 tags (max limit)
        for (int i = 0; i < 50; i++) {
            node.addTag(TagKey.of("key" + i), TagValue.of("value" + i));
        }
        
        TagKey newKey = TagKey.of("key51");
        TagValue newValue = TagValue.of("value51");
        AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
            node.getId(),
            newKey,
            newValue
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("Maximum number of tags");
        
        verify(nodeRepository, never()).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should overwrite existing tag with same key")
    void shouldOverwriteExistingTagWithSameKey() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        TagValue oldValue = TagValue.of("old");
        TagValue newValue = TagValue.of("new");
        
        node.addTag(key, oldValue);
        
        AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
            node.getId(),
            key,
            newValue
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Tag result = useCase.execute(command);
        
        // Then
        assertThat(result.getValue()).isEqualTo(newValue);
        assertThat(node.getTags().get(key).getValue()).isEqualTo(newValue);
        verify(nodeRepository).save(any(Node.class));
    }
    
    @Test
    @DisplayName("Should fail when tag key is invalid")
    void shouldFailWhenTagKeyIsInvalid() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // When/Then - Invalid tag key creation should fail
        assertThatThrownBy(() -> {
            TagKey invalidKey = TagKey.of("1invalid"); // Starts with number
            TagValue value = TagValue.of("value");
            AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
                node.getId(),
                invalidKey,
                value
            );
            useCase.execute(command);
        })
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should fail when tag value exceeds max length")
    void shouldFailWhenTagValueExceedsMaxLength() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        
        // When/Then - Tag value exceeding max length should fail
        assertThatThrownBy(() -> {
            TagKey key = TagKey.of("key");
            TagValue invalidValue = TagValue.of("a".repeat(501)); // Exceeds 500 char limit
            AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
                node.getId(),
                key,
                invalidValue
            );
            useCase.execute(command);
        })
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should normalize tag key to lowercase")
    void shouldNormalizeTagKeyToLowercase() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        TagKey key = TagKey.of("DEPARTMENT"); // Will be normalized to lowercase
        TagValue value = TagValue.of("engineering");
        
        AddTagUseCase.AddTagCommand command = new AddTagUseCase.AddTagCommand(
            node.getId(),
            key,
            value
        );
        
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Tag result = useCase.execute(command);
        
        // Then
        assertThat(result.getKey().getValue()).isEqualTo("department");
        verify(nodeRepository).save(any(Node.class));
    }
}

