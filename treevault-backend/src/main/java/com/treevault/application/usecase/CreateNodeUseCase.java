package com.treevault.application.usecase;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.domain.exception.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CreateNodeUseCase {
    
    private final NodeRepository nodeRepository;
    
    public CreateNodeUseCase(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }
    
    public Node execute(CreateNodeCommand command) {
        validateCommand(command);
        
        Node parent = null;
        if (command.getParentId() != null) {
            parent = nodeRepository.findById(command.getParentId())
                .orElseThrow(() -> new NodeNotFoundException(
                    "Parent node not found: " + command.getParentId()
                ));
            
            // Validate parent is a folder for files
            if (command.getType() == NodeType.FILE && parent.getType() != NodeType.FOLDER) {
                throw new InvalidNodeOperationException("Files can only be added to folders");
            }
        } else {
            // No parent specified: attach to the true root container
            if (command.getType() == NodeType.FILE) {
                // Safety: files must always have a parent
                throw new NodeValidationException("Files must have a parent folder");
            }
            // Ensure a single true root exists and attach new folder under it
            Node trueRoot = nodeRepository.findRootNode()
                .orElseGet(() -> nodeRepository.save(Node.createRoot()));
            parent = trueRoot;
        }
        
        NodeName nodeName = NodeName.of(command.getName());
        
        if (parent != null) {
            boolean nameExists = nodeRepository.existsByParentAndName(
                parent.getId(), nodeName
            );
            if (nameExists) {
                throw new InvalidNodeOperationException(
                    "Node with name '" + nodeName + "' already exists in parent folder"
                );
            }
        } else {
            // Prevent duplicate names at top-level
            boolean nameExistsAtTopLevel = nodeRepository.existsByParentAndName(null, nodeName);
            if (nameExistsAtTopLevel) {
                throw new InvalidNodeOperationException(
                    "Node with name '" + nodeName + "' already exists at the top level"
                );
            }
        }
        
        Node node;
        if (command.getType() == NodeType.FOLDER) {
            node = Node.createFolder(nodeName, parent);
        } else {
            node = Node.createFile(nodeName, parent);
        }
        
        if (command.getTags() != null) {
            command.getTags().forEach((key, value) -> {
                node.addTag(TagKey.of(key), TagValue.of(value));
            });
        }
        
        return nodeRepository.save(node);
    }
    
    private void validateCommand(CreateNodeCommand command) {
        if (command == null) {
            throw new NodeValidationException("Create node command cannot be null");
        }
        if (command.getName() == null || command.getName().trim().isEmpty()) {
            throw new NodeValidationException("Node name is required");
        }
        if (command.getType() == null) {
            throw new NodeValidationException("Node type is required");
        }
        if (command.getType() == NodeType.FILE && command.getParentId() == null) {
            throw new NodeValidationException("Files must have a parent folder");
        }
    }
    
    public static class CreateNodeCommand {
        private final String name;
        private final NodeType type;
        private final NodeId parentId;
        private final java.util.Map<String, String> tags;
        
        public CreateNodeCommand(String name, NodeType type, NodeId parentId, java.util.Map<String, String> tags) {
            this.name = name;
            this.type = type;
            this.parentId = parentId;
            this.tags = tags;
        }
        
        public String getName() { return name; }
        public NodeType getType() { return type; }
        public NodeId getParentId() { return parentId; }
        public java.util.Map<String, String> getTags() { return tags; }
    }
}

