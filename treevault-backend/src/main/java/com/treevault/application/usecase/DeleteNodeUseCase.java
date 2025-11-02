package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class DeleteNodeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public DeleteNodeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public void execute(NodeId nodeId) {
        Node node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + nodeId
            ));
        
        node.delete();
        nodeRepository.delete(node);
    }
}

