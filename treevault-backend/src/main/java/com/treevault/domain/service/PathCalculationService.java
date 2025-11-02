package com.treevault.domain.service;

import com.treevault.domain.model.valueobject.NodePath;
import com.treevault.domain.model.valueobject.NodeName;
import org.springframework.stereotype.Service;

@Service
public class PathCalculationService {
    
    public NodePath calculatePath(NodePath parentPath, NodeName nodeName) {
        if (parentPath == null || parentPath.isRoot()) {
            return NodePath.root().append(nodeName);
        }
        return parentPath.append(nodeName);
    }
}

