package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.Position;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class MoveNodeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public MoveNodeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public Node execute(MoveNodeCommand command) {
        Node node = nodeRepository.findById(command.getNodeId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + command.getNodeId()
            ));
        
        Node newParent = nodeRepository.findById(command.getNewParentId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Parent node not found: " + command.getNewParentId()
            ));
        
        node.moveTo(newParent, command.getNewPosition());
        
        return nodeRepository.save(node);
    }
    
    public static class MoveNodeCommand {
        private final NodeId nodeId;
        private final NodeId newParentId;
        private final Position newPosition;
        
        public MoveNodeCommand(NodeId nodeId, NodeId newParentId, Position newPosition) {
            this.nodeId = nodeId;
            this.newParentId = newParentId;
            this.newPosition = newPosition;
        }
        
        public NodeId getNodeId() { return nodeId; }
        public NodeId getNewParentId() { return newParentId; }
        public Position getNewPosition() { return newPosition; }
    }
}

