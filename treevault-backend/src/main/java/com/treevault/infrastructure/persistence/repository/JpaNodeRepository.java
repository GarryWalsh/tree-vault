package com.treevault.infrastructure.persistence.repository;

import com.treevault.infrastructure.persistence.entity.NodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNodeRepository extends JpaRepository<NodeJpaEntity, UUID> {
    Optional<NodeJpaEntity> findByParentIdIsNull();
    List<NodeJpaEntity> findByParentIdOrderByPosition(UUID parentId);
    boolean existsByParentIdAndName(UUID parentId, String name);

    @Query("select n from NodeJpaEntity n where n.path like concat(:prefix, '%') order by n.depth desc")
    List<NodeJpaEntity> findSubtreeByPathPrefixOrderByDepthDesc(@Param("prefix") String prefix);

    Optional<NodeJpaEntity> findByParentIdIsNullAndName(String name);
}

