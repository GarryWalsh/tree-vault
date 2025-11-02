package com.treevault.application.usecase;

import com.treevault.domain.exception.NodeNotFoundException;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.repository.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTreeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public GetTreeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    @Transactional
    public Node execute() {
        return nodeRepository.findRootNode()
            .orElseGet(() -> {
                Node root = Node.createRoot();
                return nodeRepository.save(root);
            });
    }
    
    @Transactional(readOnly = true)
    public Node getNode(NodeId nodeId) {
        return nodeRepository.findById(nodeId)
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + nodeId
            ));
    }
}

