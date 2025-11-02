package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.NodeNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UpdateNodeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public UpdateNodeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public Node execute(UpdateNodeCommand command) {
        Node node = nodeRepository.findById(command.getNodeId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + command.getNodeId()
            ));
        
        NodeName newName = NodeName.of(command.getNewName());
        node.rename(newName);
        
        return nodeRepository.save(node);
    }
    
    public static class UpdateNodeCommand {
        private final NodeId nodeId;
        private final String newName;
        
        public UpdateNodeCommand(NodeId nodeId, String newName) {
            this.nodeId = nodeId;
            this.newName = newName;
        }
        
        public NodeId getNodeId() { return nodeId; }
        public String getNewName() { return newName; }
    }
}

