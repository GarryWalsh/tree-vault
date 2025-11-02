package com.treevault.api.dto.request;

import com.treevault.domain.model.valueobject.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class CreateNodeRequest {
    @NotBlank(message = "Node name is required")
    @Size(min = 1, max = 255, message = "Node name must be between 1 and 255 characters")
    private String name;
    
    @NotNull(message = "Node type is required")
    private NodeType type;
    
    private String parentId;
    
    private Map<String, String> tags;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public NodeType getType() {
        return type;
    }
    
    public void setType(NodeType type) {
        this.type = type;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    public Map<String, String> getTags() {
        return tags;
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}

