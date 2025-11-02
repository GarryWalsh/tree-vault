package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RemoveTagUseCase {
    
    private final NodeRepository nodeRepository;
    
    public RemoveTagUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public void execute(NodeId nodeId, TagKey key) {
        Node node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + nodeId
            ));
        
        node.removeTag(key);
        nodeRepository.save(node);
    }
}

