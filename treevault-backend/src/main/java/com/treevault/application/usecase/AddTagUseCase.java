package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.NodeNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AddTagUseCase {
    
    private final NodeRepository nodeRepository;
    
    public AddTagUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public Tag execute(AddTagCommand command) {
        Node node = nodeRepository.findById(command.getNodeId())
            .orElseThrow(() -> new NodeNotFoundException(
                "Node not found: " + command.getNodeId()
            ));
        
        node.addTag(command.getKey(), command.getValue());
        nodeRepository.save(node);
        
        return node.getTags().get(command.getKey());
    }
    
    public static class AddTagCommand {
        private final NodeId nodeId;
        private final TagKey key;
        private final TagValue value;
        
        public AddTagCommand(NodeId nodeId, TagKey key, TagValue value) {
            this.nodeId = nodeId;
            this.key = key;
            this.value = value;
        }
        
        public NodeId getNodeId() { return nodeId; }
        public TagKey getKey() { return key; }
        public TagValue getValue() { return value; }
    }
}

