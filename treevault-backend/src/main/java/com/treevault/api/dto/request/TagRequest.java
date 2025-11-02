package com.treevault.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class TagRequest {
    @NotBlank(message = "Tag key is required")
    private String key;
    
    @NotBlank(message = "Tag value is required")
    private String value;
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}

