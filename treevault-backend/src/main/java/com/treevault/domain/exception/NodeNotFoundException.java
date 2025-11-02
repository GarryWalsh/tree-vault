package com.treevault.domain.exception;

public class NodeNotFoundException extends DomainException {
    private final String nodeId;
    
    public NodeNotFoundException(String message) {
        super(message);
        this.nodeId = null;
    }
    
    public NodeNotFoundException(String message, String nodeId) {
        super(message);
        this.nodeId = nodeId;
    }
    
    public String getNodeId() {
        return nodeId;
    }
}

