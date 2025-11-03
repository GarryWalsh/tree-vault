package com.treevault.domain.service;

import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.InvalidNodeOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeDomainServiceTest {
    
    @Mock
    private NodeRepository nodeRepository;
    
    private NodeDomainService service;
    
    @BeforeEach
    void setUp() {
        service = new NodeDomainService(nodeRepository);
    }
    
    @Test
    @DisplayName("Should pass validation when name is unique under parent")
    void shouldPassValidationWhenNameIsUniqueUnderParent() {
        // Given
        NodeId parentId = NodeId.generate();
        NodeName name = NodeName.of("UniqueName");
        
        when(nodeRepository.existsByParentAndName(parentId, name)).thenReturn(false);
        
        // When/Then - Should not throw exception
        service.validateUniqueNameUnderParent(parentId, name);
        
        verify(nodeRepository).existsByParentAndName(parentId, name);
    }
    
    @Test
    @DisplayName("Should fail validation when duplicate name exists under parent")
    void shouldFailValidationWhenDuplicateNameExistsUnderParent() {
        // Given
        NodeId parentId = NodeId.generate();
        NodeName name = NodeName.of("DuplicateName");
        
        when(nodeRepository.existsByParentAndName(parentId, name)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> service.validateUniqueNameUnderParent(parentId, name))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    @DisplayName("Should handle null parent ID for root level validation")
    void shouldHandleNullParentIdForRootLevelValidation() {
        // Given
        NodeName name = NodeName.of("RootLevelName");
        
        when(nodeRepository.existsByParentAndName(null, name)).thenReturn(false);
        
        // When/Then - Should not throw exception
        service.validateUniqueNameUnderParent(null, name);
        
        verify(nodeRepository).existsByParentAndName(null, name);
    }
    
    @Test
    @DisplayName("Should fail when duplicate name exists at root level")
    void shouldFailWhenDuplicateNameExistsAtRootLevel() {
        // Given
        NodeName name = NodeName.of("RootDuplicate");
        
        when(nodeRepository.existsByParentAndName(null, name)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> service.validateUniqueNameUnderParent(null, name))
            .isInstanceOf(InvalidNodeOperationException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    @DisplayName("Should validate case-sensitive name uniqueness")
    void shouldValidateCaseSensitiveNameUniqueness() {
        // Given
        NodeId parentId = NodeId.generate();
        NodeName name1 = NodeName.of("MyFolder");
        NodeName name2 = NodeName.of("myfolder"); // Different case
        
        when(nodeRepository.existsByParentAndName(parentId, name1)).thenReturn(false);
        when(nodeRepository.existsByParentAndName(parentId, name2)).thenReturn(false);
        
        // When/Then - Should pass for both (case-sensitive)
        service.validateUniqueNameUnderParent(parentId, name1);
        service.validateUniqueNameUnderParent(parentId, name2);
        
        verify(nodeRepository, times(2)).existsByParentAndName(eq(parentId), any(NodeName.class));
    }
}

