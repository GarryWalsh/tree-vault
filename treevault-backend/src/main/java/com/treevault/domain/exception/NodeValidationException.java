package com.treevault.domain.exception;

import java.util.Map;

public class NodeValidationException extends DomainException {
    private Map<String, String> violations;
    
    public NodeValidationException(String message) {
        super(message);
    }
    
    public NodeValidationException(String message, Map<String, String> violations) {
        super(message);
        this.violations = violations;
    }
    
    public Map<String, String> getViolations() {
        return violations;
    }
}

