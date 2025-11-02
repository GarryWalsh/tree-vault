package com.treevault.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateNodeRequest {
    @NotBlank(message = "Node name is required")
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}

