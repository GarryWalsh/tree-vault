package com.treevault.domain.model.entity;

import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import java.util.Objects;

public class Tag {
    private final TagKey key;
    private final TagValue value;
    private final Node node;
    
    public Tag(TagKey key, TagValue value, Node node) {
        this.key = Objects.requireNonNull(key, "Tag key cannot be null");
        this.value = Objects.requireNonNull(value, "Tag value cannot be null");
        this.node = Objects.requireNonNull(node, "Tag node cannot be null");
    }
    
    public TagKey getKey() {
        return key;
    }
    
    public TagValue getValue() {
        return value;
    }
    
    public Node getNode() {
        return node;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(key, tag.key) && Objects.equals(node, tag.node);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key, node);
    }
}

