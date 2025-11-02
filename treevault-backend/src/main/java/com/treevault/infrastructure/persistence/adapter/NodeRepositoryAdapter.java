package com.treevault.infrastructure.persistence.adapter;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.repository.NodeRepository;
import com.treevault.infrastructure.persistence.entity.NodeJpaEntity;
import com.treevault.infrastructure.persistence.entity.TagJpaEntity;
import com.treevault.infrastructure.persistence.repository.JpaNodeRepository;
import com.treevault.infrastructure.persistence.repository.JpaTagRepository;
import com.treevault.infrastructure.persistence.mapper.PersistenceMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class NodeRepositoryAdapter implements NodeRepository {
    
    private final JpaNodeRepository jpaRepository;
    private final JpaTagRepository jpaTagRepository;
    private final PersistenceMapper mapper;
    private final EntityManager entityManager;
    
    public NodeRepositoryAdapter(JpaNodeRepository jpaRepository,
                                 JpaTagRepository jpaTagRepository,
                                 PersistenceMapper mapper,
                                 EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.jpaTagRepository = jpaTagRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }
    
    @Override
    public Optional<Node> findById(NodeId id) {
        Optional<NodeJpaEntity> entity = jpaRepository.findById(id.getValue());
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        Optional<NodeJpaEntity> rootEntity = findTrueRootEntity();
        NodeJpaEntity rootJpaEntity = rootEntity.orElse(entity.get());
        Node root = loadFullTree(rootJpaEntity);
        return Optional.ofNullable(findInTreeById(root, id));
    }
    
    @Override
    public Node save(Node node) {
        // Persist the entire subtree rooted at this node, preserving parent linkage and positions
        NodeJpaEntity savedRoot = persistRecursively(node, null);
        // Reload from the true root and return the same node instance with proper parent linkage
        return findById(node.getId()).orElseGet(() -> loadFullTree(savedRoot));
    }
    
    private NodeJpaEntity persistRecursively(Node domainNode, NodeJpaEntity parentEntity) {
        // Map domain to JPA entity (detached)
        NodeJpaEntity mapped = mapper.toJpaEntity(domainNode);
        
        // Attach parent if provided/known
        if (parentEntity != null) {
            mapped.setParent(parentEntity);
        } else if (domainNode.getParent().isPresent()) {
            NodeJpaEntity parentFromDb = entityManager.find(NodeJpaEntity.class, domainNode.getParent().get().getId().getValue());
            mapped.setParent(parentFromDb);
        } else {
            mapped.setParent(null);
        }
        
        // Find existing managed entity to avoid duplicate identifier issues
        NodeJpaEntity saved = entityManager.find(NodeJpaEntity.class, domainNode.getId().getValue());
        if (saved != null) {
            // Update fields on the managed instance; keep version managed by JPA
            saved.setName(mapped.getName());
            saved.setType(mapped.getType());
            saved.setPath(mapped.getPath());
            saved.setDepth(mapped.getDepth());
            saved.setPosition(mapped.getPosition());
            saved.setUpdatedAt(mapped.getUpdatedAt());
            saved.setParent(mapped.getParent());
        } else {
            // Persist new entity
            saved = jpaRepository.save(mapped);
        }
        
        // Replace tags for this node
        jpaTagRepository.deleteByNodeId(saved.getId());
        if (domainNode.getTags() != null && !domainNode.getTags().isEmpty()) {
            for (Map.Entry<TagKey, Tag> tagEntry : domainNode.getTags().entrySet()) {
                Tag tag = tagEntry.getValue();
                TagJpaEntity tagEntity = TagJpaEntity.builder()
                        .node(saved)
                        .tagKey(tag.getKey().getValue())
                        .tagValue(tag.getValue().getValue())
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                jpaTagRepository.save(tagEntity);
            }
        }
        
        // Persist children in order
        for (Node child : domainNode.getChildren()) {
            persistRecursively(child, saved);
        }
        
        return saved;
    }
    
    @Override
    public void delete(Node node) {
        // Delete entire subtree by path prefix (tags first, then nodes deepest-first)
        String prefix = node.getPath().toString();
        List<NodeJpaEntity> subtree = jpaRepository.findSubtreeByPathPrefixOrderByDepthDesc(prefix);
        if (subtree.isEmpty()) {
            // Fallback: delete single by ID
            jpaRepository.deleteById(node.getId().getValue());
            return;
        }
        List<UUID> ids = subtree.stream().map(NodeJpaEntity::getId).toList();
        jpaTagRepository.deleteByNodeIdIn(ids);
        jpaRepository.deleteAll(subtree);
    }
    
    @Override
    public boolean existsByParentAndName(NodeId parentId, NodeName name) {
        return jpaRepository.existsByParentIdAndName(
            parentId != null ? parentId.getValue() : null,
            name.getValue()
        );
    }
    
    @Override
    public Optional<Node> findRootNode() {
        Optional<NodeJpaEntity> rootEntity = findTrueRootEntity();
        return rootEntity.map(this::loadFullTree);
    }
    
    @Override
    public List<Node> findByParentId(NodeId parentId) {
        Optional<NodeJpaEntity> rootEntity = findTrueRootEntity();
        if (rootEntity.isEmpty()) {
            return Collections.emptyList();
        }
        Node root = loadFullTree(rootEntity.get());
        Node parent = findInTreeById(root, parentId);
        if (parent == null) {
            return Collections.emptyList();
        }
        return parent.getChildren();
    }
    
    @Override
    public List<Node> findAll() {
        List<NodeJpaEntity> entities = jpaRepository.findAll();
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find root and load full tree
        Optional<NodeJpaEntity> rootEntity = entities.stream()
            .filter(e -> e.getParent() == null)
            .findFirst();

        return rootEntity.map(nodeJpaEntity -> Collections.singletonList(loadFullTree(nodeJpaEntity))).orElse(Collections.emptyList());

    }
    
    private Node loadFullTree(NodeJpaEntity rootEntity) {
        // Load all nodes and tags
        List<NodeJpaEntity> allEntities = jpaRepository.findAll();
        List<TagJpaEntity> allTags = entityManager.createQuery(
            "SELECT t FROM TagJpaEntity t", TagJpaEntity.class
        ).getResultList();
        
        // Build maps
        Map<UUID, NodeJpaEntity> entityMap = allEntities.stream()
            .collect(Collectors.toMap(NodeJpaEntity::getId, e -> e));
        
        Map<UUID, List<TagJpaEntity>> tagsByNodeId = allTags.stream()
            .collect(Collectors.groupingBy(t -> t.getNode().getId()));
        
        // Reconstruct tree starting from root
        return reconstructNode(rootEntity, entityMap, tagsByNodeId, null);
    }
    
    private Node reconstructNode(NodeJpaEntity entity, 
                                 Map<UUID, NodeJpaEntity> entityMap,
                                 Map<UUID, List<TagJpaEntity>> tagsByNodeId,
                                 Node parent) {
        // Use domain factory methods
        // Determine position based on domain parent: top-most (parent == null) has null position; others use stored position
        Position position = (parent == null) ? null
                : (entity.getPosition() != null ? Position.of(entity.getPosition()) : null);
        
        Node node = createNodeWithId(
            NodeId.of(entity.getId()),
            NodeName.of(entity.getName()),
            entity.getType() == NodeJpaEntity.NodeType.FOLDER ? NodeType.FOLDER : NodeType.FILE,
            parent,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion(),
            NodePath.of(entity.getPath()),
            position
        );
        
        if (parent != null) {
            parent.addChildForReconstructionPublic(node);
        }
        
        // Add tags
        List<TagJpaEntity> nodeTags = tagsByNodeId.getOrDefault(entity.getId(), Collections.emptyList());
        for (TagJpaEntity tagEntity : nodeTags) {
            try {
                node.addTag(
                    TagKey.of(tagEntity.getTagKey()),
                    TagValue.of(tagEntity.getTagValue())
                );
            } catch (Exception e) {
                // Ignore tag errors during reconstruction
            }
        }
        
        // Reconstruct children
        List<NodeJpaEntity> children = entityMap.values().stream()
            .filter(e -> e.getParent() != null && e.getParent().getId().equals(entity.getId()))
            .sorted(Comparator.comparing(NodeJpaEntity::getPosition))
            .toList();
        
        for (NodeJpaEntity childEntity : children) {
            reconstructNode(childEntity, entityMap, tagsByNodeId, node);
        }
        
        return node;
    }
    
    // Helper to create node with existing ID (for reconstruction)
    private Node createNodeWithId(NodeId id, NodeName name, NodeType type, Node parent,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, Long version,
                                  NodePath path, Position position) {
        // Use static factory method for reconstruction
        return Node.reconstruct(id, name, type, parent, createdAt, updatedAt, version, path, position);
    }
    
    // Helper to find a node by id within a reconstructed domain tree
    private Node findInTreeById(Node root, NodeId id) {
        if (root == null || id == null) {
            return null;
        }
        if (root.getId().equals(id)) {
            return root;
        }
        for (Node child : root.getChildren()) {
            Node found = findInTreeById(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
    
    // Find the single true root entity (name = 'root'). Do NOT create it here; creation is handled by use cases.
    private Optional<NodeJpaEntity> findTrueRootEntity() {
        Optional<NodeJpaEntity> root = jpaRepository.findByParentIdIsNullAndName("root");
        if (root.isPresent()) {
            return root;
        }
        return jpaRepository.findByParentIdIsNull();
    }
}
