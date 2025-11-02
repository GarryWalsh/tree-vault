package com.treevault.domain.model.entity;

import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTest {
    
    @Test
    @DisplayName("Should create tag with valid key and value")
    void shouldCreateTagWithValidKeyAndValue() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        
        // When
        Tag tag = new Tag(key, value, node);
        
        // Then
        assertThat(tag).isNotNull();
        assertThat(tag.getKey()).isEqualTo(key);
        assertThat(tag.getValue()).isEqualTo(value);
        assertThat(tag.getNode()).isEqualTo(node);
    }
    
    @Test
    @DisplayName("Should fail when tag key is null")
    void shouldFailWhenTagKeyIsNull() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagValue value = TagValue.of("engineering");
        
        // When/Then
        assertThatThrownBy(() -> new Tag(null, value, node))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Tag key cannot be null");
    }
    
    @Test
    @DisplayName("Should fail when tag value is null")
    void shouldFailWhenTagValueIsNull() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        
        // When/Then
        assertThatThrownBy(() -> new Tag(key, null, node))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Tag value cannot be null");
    }
    
    @Test
    @DisplayName("Should fail when node is null")
    void shouldFailWhenNodeIsNull() {
        // Given
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        
        // When/Then
        assertThatThrownBy(() -> new Tag(key, value, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Tag node cannot be null");
    }
    
    @Test
    @DisplayName("Should be equal when key and node are the same")
    void shouldBeEqualWhenKeyAndNodeAreTheSame() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("department");
        TagValue value1 = TagValue.of("engineering");
        TagValue value2 = TagValue.of("sales");
        
        Tag tag1 = new Tag(key, value1, node);
        Tag tag2 = new Tag(key, value2, node); // Same key and node, different value
        
        // When/Then
        assertThat(tag1).isEqualTo(tag2);
        assertThat(tag1.hashCode()).isEqualTo(tag2.hashCode());
    }
    
    @Test
    @DisplayName("Should not be equal when key is different")
    void shouldNotBeEqualWhenKeyIsDifferent() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key1 = TagKey.of("department");
        TagKey key2 = TagKey.of("status");
        TagValue value = TagValue.of("engineering");
        
        Tag tag1 = new Tag(key1, value, node);
        Tag tag2 = new Tag(key2, value, node);
        
        // When/Then
        assertThat(tag1).isNotEqualTo(tag2);
    }
    
    @Test
    @DisplayName("Should not be equal when node is different")
    void shouldNotBeEqualWhenNodeIsDifferent() {
        // Given
        Node node1 = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder1"), null);
        Node node2 = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder2"), null);
        TagKey key = TagKey.of("department");
        TagValue value = TagValue.of("engineering");
        
        Tag tag1 = new Tag(key, value, node1);
        Tag tag2 = new Tag(key, value, node2);
        
        // When/Then
        assertThat(tag1).isNotEqualTo(tag2);
    }
    
    @Test
    @DisplayName("Should handle tag with empty value")
    void shouldHandleTagWithEmptyValue() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("note");
        TagValue emptyValue = TagValue.of("");
        
        // When
        Tag tag = new Tag(key, emptyValue, node);
        
        // Then
        assertThat(tag.getValue().getValue()).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle tag with maximum length value")
    void shouldHandleTagWithMaximumLengthValue() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("description");
        String maxValue = "a".repeat(500); // Max length
        TagValue maxLengthValue = TagValue.of(maxValue);
        
        // When
        Tag tag = new Tag(key, maxLengthValue, node);
        
        // Then
        assertThat(tag.getValue().getValue()).hasSize(500);
    }
    
    @Test
    @DisplayName("Should normalize tag key to lowercase")
    void shouldNormalizeTagKeyToLowercase() {
        // Given
        Node node = Node.createFolder(com.treevault.domain.model.valueobject.NodeName.of("Folder"), null);
        TagKey key = TagKey.of("DEPARTMENT"); // Will be normalized
        TagValue value = TagValue.of("engineering");
        
        // When
        Tag tag = new Tag(key, value, node);
        
        // Then
        assertThat(tag.getKey().getValue()).isEqualTo("department");
    }
}

