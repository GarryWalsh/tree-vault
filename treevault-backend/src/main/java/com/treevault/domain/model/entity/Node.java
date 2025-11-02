package com.treevault.domain.model.entity;

import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.exception.*;
import java.time.LocalDateTime;
import java.util.*;

public class Node {
    private final NodeId id;
    private NodeName name;
    private final NodeType type;
    private Node parent;
    private final List<Node> children;
    private final Map<TagKey, Tag> tags;
    private NodePath path;
    private Position position;
    private Long version;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Node(NodeId id, NodeName name, NodeType type, Node parent) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.parent = parent;
        this.children = new ArrayList<>();
        this.tags = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
        
        calculatePath();
        validateNodeTypeConstraints();
    }
    
    // Package-private constructor for reconstruction from persistence
    Node(NodeId id, NodeName name, NodeType type, Node parent, 
         LocalDateTime createdAt, LocalDateTime updatedAt, Long version, 
         NodePath path, Position position) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.parent = parent;
        this.children = new ArrayList<>();
        this.tags = new HashMap<>();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.version = version != null ? version : 0L;
        this.path = path != null ? path : NodePath.root();
        this.position = position;
        
        validateNodeTypeConstraints();
    }
    
    public static Node createFolder(NodeName name, Node parent) {
        Node node = new Node(NodeId.generate(), name, NodeType.FOLDER, parent);
        if (parent != null) {
            parent.addChild(node);
        }
        return node;
    }
    
    public static Node createFile(NodeName name, Node parent) {
        if (parent == null) {
            throw new InvalidNodeOperationException("Files must have a parent folder");
        }
        if (parent.getType() != NodeType.FOLDER) {
            throw new InvalidNodeOperationException("Files can only be added to folders");
        }
        
        Node node = new Node(NodeId.generate(), name, NodeType.FILE, parent);
        parent.addChild(node);
        return node;
    }
    
    public static Node createRoot() {
        return new Node(NodeId.generate(), NodeName.of("root"), NodeType.FOLDER, null);
    }
    
    // Factory method for reconstruction from persistence (allows infrastructure layer to rebuild domain model)
    public static Node reconstruct(NodeId id, NodeName name, NodeType type, Node parent,
                                   LocalDateTime createdAt, LocalDateTime updatedAt, Long version,
                                   NodePath path, Position position) {
        return new Node(id, name, type, parent, createdAt, updatedAt, version, path, position);
    }
    
    public void rename(NodeName newName) {
        validateNotNull(newName, "New name");
        
        if (this.name.equals(newName)) {
            return;
        }
        
        if (parent != null) {
            boolean nameExists = parent.children.stream()
                .filter(child -> !child.equals(this))
                .anyMatch(child -> child.name.equals(newName));
                
            if (nameExists) {
                throw new InvalidNodeOperationException(
                    "A node with name '" + newName + "' already exists in the same folder"
                );
            }
        }
        
        this.name = newName;
        updatePathRecursively();
        markUpdated();
    }
    
    public void moveTo(Node newParent, Position newPosition) {
        validateNotNull(newParent, "New parent");
        validateNotNull(newPosition, "New position");
        
        if (this.type == NodeType.FILE && newParent.type != NodeType.FOLDER) {
            throw new InvalidNodeOperationException("Files can only be moved to folders");
        }
        
        if (this.equals(newParent)) {
            throw new InvalidNodeOperationException("Cannot move node to itself");
        }
        
        if (newParent.isDescendantOf(this)) {
            throw new CircularReferenceException("Cannot move node to its own descendant");
        }
        
        // Check for name conflicts, excluding the node being moved (for same-parent reordering)
        boolean nameConflict = newParent.children.stream()
            .filter(child -> !child.equals(this))
            .anyMatch(child -> child.name.equals(this.name));
            
        if (nameConflict) {
            throw new InvalidNodeOperationException(
                "A node with name '" + this.name + "' already exists in the target folder"
            );
        }
        
        boolean sameParent = this.parent != null && this.parent.equals(newParent);
        
        if (sameParent) {
            // Special handling for same-parent reordering to avoid position conflicts
            newParent.reorderChild(this, newPosition);
        } else {
            // Different parent: remove from old, add to new
            if (this.parent != null) {
                this.parent.removeChild(this);
            }
            
            this.parent = newParent;
            newParent.addChildAt(this, newPosition);
        }
        
        updatePathRecursively();
        markUpdated();
    }
    
    public void addTag(TagKey key, TagValue value) {
        validateNotNull(key, "Tag key");
        validateNotNull(value, "Tag value");
        
        if (tags.size() >= 50) {
            throw new NodeValidationException("Maximum number of tags (50) exceeded");
        }
        
        Tag tag = new Tag(key, value, this);
        tags.put(key, tag);
        markUpdated();
    }
    
    public void removeTag(TagKey key) {
        validateNotNull(key, "Tag key");
        
        if (!tags.containsKey(key)) {
            throw new NodeNotFoundException("Tag with key '" + key + "' not found");
        }
        
        tags.remove(key);
        markUpdated();
    }
    
    public void delete() {
        if (isRoot()) {
            throw new InvalidNodeOperationException("Cannot delete the root node");
        }
        
        new ArrayList<>(children).forEach(Node::delete);
        
        if (parent != null) {
            parent.removeChild(this);
        }
        
        children.clear();
        tags.clear();
    }
    
    private void addChild(Node child) {
        validateNodeTypeConstraints();
        
        if (this.type != NodeType.FOLDER) {
            throw new InvalidNodeOperationException("Only folders can have children");
        }
        
        if (!children.contains(child)) {
            children.add(child);
            child.position = Position.of(children.size() - 1);
        }
    }
    
    // Package-private method for reconstruction (skips validation)
    void addChildForReconstruction(Node child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }
    
    public void addChildForReconstructionPublic(Node child) {
        addChildForReconstruction(child);
    }
    
    private void addChildAt(Node child, Position position) {
        validateNodeTypeConstraints();
        
        if (position.getValue() > children.size()) {
            throw new NodeValidationException("Invalid position: " + position);
        }
        
        children.add(position.getValue(), child);
        reindexChildren();
    }
    
    private void removeChild(Node child) {
        children.remove(child);
        reindexChildren();
    }
    
    private void reorderChild(Node child, Position newPosition) {
        if (!children.contains(child)) {
            throw new InvalidNodeOperationException("Cannot reorder child that doesn't belong to this parent");
        }
        
        int oldIndex = children.indexOf(child);
        int newIndex = newPosition.getValue();
        
        if (oldIndex == newIndex) {
            return; // Already in the correct position
        }
        
        if (newIndex < 0 || newIndex >= children.size()) {
            throw new NodeValidationException("Invalid position: " + newPosition);
        }
        
        // Reorder in memory
        children.remove(oldIndex);
        children.add(newIndex, child);
        
        // Reindex to correct final positions
        reindexChildren();
    }
    
    private void reindexChildren() {
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).position = Position.of(i);
            if (!positions.add(i)) {
                throw new InvalidNodeOperationException("Duplicate position detected during reindexing");
            }
        }
    }
    
    private void calculatePath() {
        if (parent == null) {
            this.path = NodePath.root();
        } else {
            this.path = parent.path.append(this.name);
        }
    }
    
    private void updatePathRecursively() {
        calculatePath();
        children.forEach(Node::updatePathRecursively);
    }
    
    public boolean isDescendantOf(Node other) {
        Node current = this.parent;
        while (current != null) {
            if (current.equals(other)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }
    
    private void validateNodeTypeConstraints() {
        if (this.type == NodeType.FILE && !children.isEmpty()) {
            throw new InvalidNodeOperationException("Files cannot have children");
        }
    }
    
    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new NodeValidationException(fieldName + " cannot be null");
        }
    }
    
    private void markUpdated() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    public boolean isFolder() {
        return type == NodeType.FOLDER;
    }
    
    public boolean isFile() {
        return type == NodeType.FILE;
    }
    
    public NodeId getId() { return id; }
    public NodeName getName() { return name; }
    public NodeType getType() { return type; }
    public Optional<Node> getParent() { return Optional.ofNullable(parent); }
    public List<Node> getChildren() { return Collections.unmodifiableList(children); }
    public Map<TagKey, Tag> getTags() { return Collections.unmodifiableMap(tags); }
    public NodePath getPath() { return path; }
    public Position getPosition() { return position; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

