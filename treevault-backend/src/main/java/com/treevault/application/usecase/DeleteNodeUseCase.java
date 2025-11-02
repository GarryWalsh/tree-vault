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
        
        // Get parent before deleting (to save reindexed positions later)
        Node parent = node.getParent().orElse(null);
        
        node.delete();  // This removes the node from parent and reindexes siblings
        nodeRepository.delete(node);
        
        // Save parent to persist reindexed positions of remaining siblings
        if (parent != null) {
            nodeRepository.save(parent);
        }
    }
}

