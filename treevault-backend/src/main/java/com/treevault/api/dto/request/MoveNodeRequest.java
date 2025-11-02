package com.treevault.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MoveNodeRequest {
    @NotBlank(message = "New parent ID is required")
    private String newParentId;
    
    @NotNull(message = "Position is required")
    @Min(value = 0, message = "Position must be non-negative")
    private Integer position;
    
    public String getNewParentId() {
        return newParentId;
    }
    
    public void setNewParentId(String newParentId) {
        this.newParentId = newParentId;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
}

