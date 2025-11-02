package com.treevault.infrastructure.persistence.mapper;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodePath;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.model.valueobject.Position;
import com.treevault.infrastructure.persistence.entity.NodeJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PersistenceMapper {

    /**
     * Maps a domain {@link Node} to its JPA representation.
     * Parent/child relationships are handled separately in {@code NodeRepositoryAdapter}.
     */
    public NodeJpaEntity toJpaEntity(Node node) {
        if (node == null) {
            return null;
        }

        NodeJpaEntity entity = new NodeJpaEntity();
        entity.setId(toUuid(node.getId()));
        entity.setName(toName(node.getName()));
        entity.setType(toJpaType(node.getType()));
        entity.setPath(toPath(node.getPath()));
        entity.setDepth(node.getPath() != null ? node.getPath().getDepth() : 0);
        entity.setPosition(toPosition(node.getPosition()));
        entity.setCreatedAt(copyDate(node.getCreatedAt()));
        entity.setUpdatedAt(copyDate(node.getUpdatedAt()));

        // Parent linkage and version management occur within the repository adapter
        return entity;
    }

    private UUID toUuid(NodeId value) {
        return value != null ? value.getValue() : null;
    }

    private String toName(NodeName value) {
        return value != null ? value.getValue() : null;
    }

    private NodeJpaEntity.NodeType toJpaType(NodeType type) {
        return type != null ? NodeJpaEntity.NodeType.valueOf(type.name()) : null;
    }

    private String toPath(NodePath path) {
        return path != null ? path.toString() : null;
    }

    private Integer toPosition(Position position) {
        return position != null ? position.getValue() : 0;
    }

    private LocalDateTime copyDate(LocalDateTime dateTime) {
        return dateTime;
    }
}
