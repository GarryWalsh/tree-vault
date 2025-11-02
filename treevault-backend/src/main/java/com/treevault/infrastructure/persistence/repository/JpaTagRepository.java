package com.treevault.infrastructure.persistence.repository;

import com.treevault.infrastructure.persistence.entity.TagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTagRepository extends JpaRepository<TagJpaEntity, UUID> {
    List<TagJpaEntity> findByNodeId(UUID nodeId);
    void deleteByNodeId(UUID nodeId);
    void deleteByNodeIdIn(List<UUID> nodeIds);
}

