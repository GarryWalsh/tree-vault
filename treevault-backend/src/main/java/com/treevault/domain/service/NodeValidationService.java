package com.treevault.domain.service;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeName;
import org.springframework.stereotype.Service;

@Service
public class NodeValidationService {
    
    public void validateNodeName(NodeName name) {
        // Validation is handled by NodeName value object
        // This service can be extended for additional business rules
    }
    
    public void validateNodeForMove(Node node, Node targetParent) {
        if (node.equals(targetParent)) {
            throw new com.treevault.domain.exception.InvalidNodeOperationException(
                "Cannot move node to itself"
            );
        }
        
        if (targetParent.isDescendantOf(node)) {
            throw new com.treevault.domain.exception.CircularReferenceException(
                "Cannot move node to its own descendant"
            );
        }
    }
}

