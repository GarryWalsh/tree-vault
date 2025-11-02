package com.treevault.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public final class NodeId {
    private final UUID value;
    
    private NodeId(UUID value) {
        this.value = value;
    }
    
    public static NodeId generate() {
        return new NodeId(UUID.randomUUID());
    }
    
    public static NodeId of(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return new NodeId(uuid);
    }
    
    public static NodeId of(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }
        return new NodeId(UUID.fromString(uuid));
    }
    
    public UUID getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(value, nodeId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}

