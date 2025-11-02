package com.treevault.api.dto.response;

public class TagResponse {
    private String key;
    private String value;
    
    public TagResponse() {
    }
    
    public TagResponse(String key, String value) {
        this.key = key;
        this.value = value;
    }
    
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

