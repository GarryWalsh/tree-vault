package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.Position;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@Transactional
public class MoveNodeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public MoveNodeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public Node execute(MoveNodeCommand command) {
        // Load the root to ensure we have a consistent object graph
        Node root = nodeRepository.findRootNode()
            .orElseThrow(() -> new NodeNotFoundException("Root node not found"));
        
        // Find both nodes within the same object graph
        Node node = findNodeInTree(root, command.getNodeId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + command.getNodeId()
            ));
        
        Node newParent = findNodeInTree(root, command.getNewParentId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Parent node not found: " + command.getNewParentId()
            ));
        
        node.moveTo(newParent, command.getNewPosition());
        
        return nodeRepository.save(node);
    }
    
    private Optional<Node> findNodeInTree(Node root, NodeId targetId) {
        if (root.getId().equals(targetId)) {
            return Optional.of(root);
        }
        for (Node child : root.getChildren()) {
            Optional<Node> found = findNodeInTree(child, targetId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
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

