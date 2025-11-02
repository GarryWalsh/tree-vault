package com.treevault.domain.model.valueobject;

import com.treevault.domain.exception.NodeValidationException;
import com.treevault.domain.exception.InvalidNodeOperationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class NodePath {
    private static final String SEPARATOR = "/";
    private static final int MAX_DEPTH = 50;
    
    private final List<String> segments;
    private final String fullPath;
    
    private NodePath(List<String> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        this.fullPath = SEPARATOR + String.join(SEPARATOR, segments) + SEPARATOR;
    }
    
    public static NodePath root() {
        return new NodePath(Collections.emptyList());
    }
    
    public static NodePath of(String path) {
        if (path == null || path.isEmpty() || path.equals(SEPARATOR) || path.equals(SEPARATOR + SEPARATOR)) {
            return root();
        }
        
        validatePath(path);
        List<String> segments = parsePath(path);
        return new NodePath(segments);
    }
    
    public NodePath append(NodeName name) {
        // Check if the new node would exceed max depth
        // MAX_DEPTH=50 means we allow depth 0-49, so prevent creating children when parent is at depth >= 49
        if (segments.size() >= MAX_DEPTH - 1) {
            throw new NodeValidationException("Maximum tree depth (" + (MAX_DEPTH - 1) + ") exceeded");
        }
        
        List<String> newSegments = new ArrayList<>(segments);
        newSegments.add(name.getValue());
        return new NodePath(newSegments);
    }
    
    public NodePath getParentPath() {
        if (isRoot()) {
            throw new InvalidNodeOperationException("Root node has no parent");
        }
        
        List<String> parentSegments = segments.subList(0, segments.size() - 1);
        return new NodePath(parentSegments);
    }
    
    public boolean isAncestorOf(NodePath other) {
        return other.fullPath.startsWith(this.fullPath) && !this.equals(other);
    }
    
    public boolean isDescendantOf(NodePath other) {
        return this.fullPath.startsWith(other.fullPath) && !this.equals(other);
    }
    
    public boolean isRoot() {
        return segments.isEmpty();
    }
    
    public int getDepth() {
        return segments.size();
    }
    
    private static void validatePath(String path) {
        if (!path.startsWith(SEPARATOR) || !path.endsWith(SEPARATOR)) {
            throw new NodeValidationException("Path must start and end with separator");
        }
        
        if (path.contains("//")) {
            throw new NodeValidationException("Path contains empty segments");
        }
    }
    
    private static List<String> parsePath(String path) {
        String trimmed = path.substring(1, path.length() - 1);
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(trimmed.split(SEPARATOR));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodePath nodePath = (NodePath) o;
        return Objects.equals(fullPath, nodePath.fullPath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fullPath);
    }
    
    @Override
    public String toString() {
        return fullPath;
    }
}

