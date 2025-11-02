package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeNameTest {
    
    @Test
    @DisplayName("Should create valid node name")
    void shouldCreateValidNodeName() {
        // When
        NodeName name = NodeName.of("Valid Name 123");
        
        // Then
        assertThat(name.getValue()).isEqualTo("Valid Name 123");
    }
    
    @Test
    @DisplayName("Should normalize whitespace in name")
    void shouldNormalizeWhitespace() {
        // When
        NodeName name = NodeName.of("  Multiple   Spaces   ");
        
        // Then
        assertThat(name.getValue()).isEqualTo("Multiple Spaces");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Should reject empty or whitespace names")
    void shouldRejectEmptyNames(String invalidName) {
        assertThatThrownBy(() -> NodeName.of(invalidName))
            .isInstanceOf(NodeValidationException.class)
            .hasMessage("Node name cannot be null or empty");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "name/with/slash",
        "name\\with\\backslash",
        "name:with:colon",
        "name*with*asterisk",
        "name?with?question",
        "name\"with\"quote",
        "name<with>brackets",
        "name|with|pipe"
    })
    @DisplayName("Should reject names with invalid characters")
    void shouldRejectInvalidCharacters(String invalidName) {
        assertThatThrownBy(() -> NodeName.of(invalidName))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("invalid characters");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {".", ".."})
    @DisplayName("Should reject reserved names")
    void shouldRejectReservedNames(String reservedName) {
        assertThatThrownBy(() -> NodeName.of(reservedName))
            .isInstanceOf(NodeValidationException.class)
            .hasMessage("Node name cannot be '.' or '..'");
    }
    
    @Test
    @DisplayName("Should reject names exceeding max length")
    void shouldRejectLongNames() {
        String longName = "a".repeat(256);
        
        assertThatThrownBy(() -> NodeName.of(longName))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("must be between 1 and 255 characters");
    }
    
    @Test
    @DisplayName("Should handle Unicode characters")
    void shouldHandleUnicodeCharacters() {
        NodeName name1 = NodeName.of("测试");
        NodeName name2 = NodeName.of("Café");
        NodeName name3 = NodeName.of("Русский");
        
        assertThat(name1.getValue()).isEqualTo("测试");
        assertThat(name2.getValue()).isEqualTo("Café");
        assertThat(name3.getValue()).isEqualTo("Русский");
    }
    
    @Test
    @DisplayName("Should handle names with Unicode emoji")
    void shouldHandleNamesWithUnicodeEmoji() {
        NodeName name = NodeName.of("Folder 📁");
        assertThat(name.getValue()).isEqualTo("Folder 📁");
    }
    
    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
        assertThatThrownBy(() -> NodeName.of(null))
            .isInstanceOf(NodeValidationException.class)
            .hasMessageContaining("cannot be null");
    }
    
    @Test
    @DisplayName("Should handle names with special Unicode combinations")
    void shouldHandleNamesWithSpecialUnicodeCombinations() {
        NodeName name1 = NodeName.of("file-ñame.txt");
        NodeName name2 = NodeName.of("文档.docx");
        NodeName name3 = NodeName.of("αβγδε");
        
        assertThat(name1.getValue()).isEqualTo("file-ñame.txt");
        assertThat(name2.getValue()).isEqualTo("文档.docx");
        assertThat(name3.getValue()).isEqualTo("αβγδε");
    }
    
    @Test
    @DisplayName("Should reject names with control characters")
    void shouldRejectNamesWithControlCharacters() {
        assertThatThrownBy(() -> NodeName.of("file\u0000name.txt"))
            .isInstanceOf(NodeValidationException.class);
        
        assertThatThrownBy(() -> NodeName.of("file\u0001name.txt"))
            .isInstanceOf(NodeValidationException.class);
    }
    
    @Test
    @DisplayName("Should handle edge case length values")
    void shouldHandleEdgeCaseLengthValues() {
        // Minimum length (1 character)
        NodeName minName = NodeName.of("a");
        assertThat(minName.getValue()).hasSize(1);
        
        // Maximum length (255 characters)
        String maxName = "a".repeat(255);
        NodeName maxNodeName = NodeName.of(maxName);
        assertThat(maxNodeName.getValue()).hasSize(255);
    }
    
    @Test
    @DisplayName("Should normalize extreme whitespace")
    void shouldNormalizeExtremeWhitespace() {
        NodeName name1 = NodeName.of("\t\t\tname\t\t\t");
        NodeName name2 = NodeName.of("   \n\n   name   \n\n   ");
        
        assertThat(name1.getValue()).isEqualTo("name");
        assertThat(name2.getValue()).isEqualTo("name");
    }
}

