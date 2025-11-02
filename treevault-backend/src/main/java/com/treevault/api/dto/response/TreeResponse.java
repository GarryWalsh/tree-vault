package com.treevault.api.dto.response;

public class TreeResponse {
    private NodeResponse root;
    
    public TreeResponse() {
    }
    
    public TreeResponse(NodeResponse root) {
        this.root = root;
    }
    
    public NodeResponse getRoot() {
        return root;
    }
    
    public void setRoot(NodeResponse root) {
        this.root = root;
    }
}

