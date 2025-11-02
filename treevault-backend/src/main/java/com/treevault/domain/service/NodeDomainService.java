package com.treevault.domain.service;

import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.repository.NodeRepository;
import org.springframework.stereotype.Service;

@Service
public class NodeDomainService {
    
    private final NodeRepository nodeRepository;
    
    public NodeDomainService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public void validateUniqueNameUnderParent(NodeId parentId, NodeName name) {
        if (nodeRepository.existsByParentAndName(parentId, name)) {
            throw new com.treevault.domain.exception.InvalidNodeOperationException(
                "Node with name '" + name + "' already exists in parent"
            );
        }
    }
}

