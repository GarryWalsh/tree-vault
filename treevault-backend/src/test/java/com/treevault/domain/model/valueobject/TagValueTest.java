package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class TagValueTest {
    
    @Test
    @DisplayName("Should create valid tag value")
    void shouldCreateValidTagValue() {
        TagValue value = TagValue.of("engineering");
        assertThat(value.getValue()).isEqualTo("engineering");
    }
    
    @Test
    @DisplayName("Should reject null tag value")
    void shouldRejectNullTagValue() {
        assertThatThrownBy(() -> TagValue.of(null))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot be null");
    }
    
    @Test
    @DisplayName("Should reject tag values exceeding max length")
    void shouldRejectTagValuesExceedingMaxLength() {
        String longValue = "a".repeat(501);
        assertThatThrownBy(() -> TagValue.of(longValue))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot exceed");
    }
    
    @Test
    @DisplayName("Should accept empty string tag value")
    void shouldAcceptEmptyStringTagValue() {
        TagValue value = TagValue.of("");
        assertThat(value.getValue()).isEqualTo("");
    }
    
    @Test
    @DisplayName("Should handle large payload handling")
    void shouldHandleLargePayloadHandling() {
        // Maximum length (500 characters)
        String maxValue = "a".repeat(500);
        TagValue maxTagValue = TagValue.of(maxValue);
        assertThat(maxTagValue.getValue()).hasSize(500);
        
        // Exceeding maximum should fail
        String tooLongValue = "a".repeat(501);
        assertThatThrownBy(() -> TagValue.of(tooLongValue))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot exceed");
    }
    
    @Test
    @DisplayName("Should handle encoding issues")
    void shouldHandleEncodingIssues() {
        // Should handle various encodings correctly
        TagValue value1 = TagValue.of("Café");
        TagValue value2 = TagValue.of("测试");
        TagValue value3 = TagValue.of("Русский");
        
        assertThat(value1.getValue()).isEqualTo("Café");
        assertThat(value2.getValue()).isEqualTo("测试");
        assertThat(value3.getValue()).isEqualTo("Русский");
    }
    
    @Test
    @DisplayName("Should handle special characters in tag value")
    void shouldHandleSpecialCharactersInTagValue() {
        // Tag values can contain special characters
        TagValue value1 = TagValue.of("value with spaces");
        TagValue value2 = TagValue.of("value@with@special");
        TagValue value3 = TagValue.of("value/with/slashes");
        
        assertThat(value1.getValue()).isEqualTo("value with spaces");
        assertThat(value2.getValue()).isEqualTo("value@with@special");
        assertThat(value3.getValue()).isEqualTo("value/with/slashes");
    }
    
    @Test
    @DisplayName("Should handle JSON-like content")
    void shouldHandleJsonLikeContent() {
        String jsonContent = "{\"key\":\"value\",\"number\":123}";
        TagValue value = TagValue.of(jsonContent);
        assertThat(value.getValue()).isEqualTo(jsonContent);
    }
    
    @Test
    @DisplayName("Should handle XML-like content")
    void shouldHandleXmlLikeContent() {
        String xmlContent = "<tag>content</tag>";
        TagValue value = TagValue.of(xmlContent);
        assertThat(value.getValue()).isEqualTo(xmlContent);
    }
    
    @Test
    @DisplayName("Should handle edge case length values")
    void shouldHandleEdgeCaseLengthValues() {
        // Minimum length (empty string)
        TagValue emptyValue = TagValue.of("");
        assertThat(emptyValue.getValue()).isEmpty();
        
        // Maximum length (500 characters)
        String maxValue = "a".repeat(500);
        TagValue maxTagValue = TagValue.of(maxValue);
        assertThat(maxTagValue.getValue()).hasSize(500);
    }
    
    @Test
    @DisplayName("Should handle multiline content")
    void shouldHandleMultilineContent() {
        String multilineContent = "Line 1\nLine 2\nLine 3";
        TagValue value = TagValue.of(multilineContent);
        assertThat(value.getValue()).isEqualTo(multilineContent);
    }
}


