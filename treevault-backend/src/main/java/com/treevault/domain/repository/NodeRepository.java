package com.treevault.domain.repository;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import java.util.List;
import java.util.Optional;

public interface NodeRepository {
    Optional<Node> findById(NodeId id);
    Node save(Node node);
    void delete(Node node);
    boolean existsByParentAndName(NodeId parentId, NodeName name);
    Optional<Node> findRootNode();
    List<Node> findByParentId(NodeId parentId);
    List<Node> findAll();
}

