package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class TagKeyTest {
    
    @Test
    @DisplayName("Should create valid tag key")
    void shouldCreateValidTagKey() {
        TagKey key = TagKey.of("department");
        assertThat(key.getValue()).isEqualTo("department");
    }
    
    @Test
    @DisplayName("Should normalize tag key to lowercase")
    void shouldNormalizeTagKeyToLowercase() {
        TagKey key = TagKey.of("DEPARTMENT");
        assertThat(key.getValue()).isEqualTo("department");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "1invalid", "-invalid", ".invalid"})
    @DisplayName("Should reject invalid tag keys")
    void shouldRejectInvalidTagKeys(String invalidKey) {
        assertThatThrownBy(() -> TagKey.of(invalidKey))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should reject tag keys exceeding max length")
    void shouldRejectTagKeysExceedingMaxLength() {
        String longKey = "a".repeat(101);
        assertThatThrownBy(() -> TagKey.of(longKey))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot exceed");
    }
    
    @Test
    @DisplayName("Should accept valid tag keys with dots, hyphens, underscores")
    void shouldAcceptValidTagKeysWithSpecialChars() {
        TagKey key1 = TagKey.of("department.engineering");
        TagKey key2 = TagKey.of("department-engineering");
        TagKey key3 = TagKey.of("department_engineering");
        
        assertThat(key1.getValue()).isEqualTo("department.engineering");
        assertThat(key2.getValue()).isEqualTo("department-engineering");
        assertThat(key3.getValue()).isEqualTo("department_engineering");
    }
    
    @Test
    @DisplayName("Should prevent SQL injection attempts")
    void shouldPreventSqlInjectionAttempts() {
        // SQL injection patterns should be rejected or sanitized
        assertThatThrownBy(() -> TagKey.of("'; DROP TABLE nodes; --"))
            .isInstanceOf(NodeValidationException.class);
        
        assertThatThrownBy(() -> TagKey.of("1' OR '1'='1"))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should prevent XSS patterns")
    void shouldPreventXssPatterns() {
        // XSS patterns should be rejected
        assertThatThrownBy(() -> TagKey.of("<script>alert('xss')</script>"))
            .isInstanceOf(NodeValidationException.class);
        
        assertThatThrownBy(() -> TagKey.of("javascript:alert('xss')"))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle special character combinations")
    void shouldHandleSpecialCharacterCombinations() {
        // Valid special characters
        TagKey key1 = TagKey.of("key.with.dots");
        TagKey key2 = TagKey.of("key-with-hyphens");
        TagKey key3 = TagKey.of("key_with_underscores");
        
        assertThat(key1.getValue()).isEqualTo("key.with.dots");
        assertThat(key2.getValue()).isEqualTo("key-with-hyphens");
        assertThat(key3.getValue()).isEqualTo("key_with_underscores");
        
        // Invalid special characters should be rejected
        assertThatThrownBy(() -> TagKey.of("key with spaces"))
            .isInstanceOf(NodeValidationException.class);
        
        assertThatThrownBy(() -> TagKey.of("key@with@at"))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle edge case length values")
    void shouldHandleEdgeCaseLengthValues() {
        // Minimum length (1 character)
        TagKey minKey = TagKey.of("a");
        assertThat(minKey.getValue()).hasSize(1);
        
        // Maximum length (100 characters)
        String maxKey = "a".repeat(100);
        TagKey maxTagKey = TagKey.of(maxKey);
        assertThat(maxTagKey.getValue()).hasSize(100);
        
        // Exceeding maximum should fail
        String tooLongKey = "a".repeat(101);
        assertThatThrownBy(() -> TagKey.of(tooLongKey))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should reject null tag key")
    void shouldRejectNullTagKey() {
        assertThatThrownBy(() -> TagKey.of(null))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot be null");
    }
}

