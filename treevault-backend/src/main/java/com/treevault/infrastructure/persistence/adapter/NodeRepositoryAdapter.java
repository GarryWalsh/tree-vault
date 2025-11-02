package com.treevault.infrastructure.persistence.adapter;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodePath;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.model.valueobject.Position;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    @Transactional
    public Node save(Node node) {
        // Navigate up to find the root of the in-memory tree
        // After domain operations like moveTo(), the root should have the correct state
        Node root = node;
        while (root.getParent().isPresent()) {
            root = root.getParent().get();
        }
        
        // Persist the entire tree from root
        // This saves all the position changes made by domain operations
        persistRecursively(root, null);
        entityManager.flush();
        
        // Return the original node that was passed in
        return node;
    }
    
    private void persistRecursively(Node domainNode, NodeJpaEntity parentEntity) {
        // Find existing managed entity
        NodeJpaEntity entity = entityManager.find(NodeJpaEntity.class, domainNode.getId().getValue());
        
        if (entity != null) {
            // Update existing entity
            entity.setName(domainNode.getName().getValue());
            entity.setPath(domainNode.getPath().toString());
            entity.setDepth(domainNode.getPath().getDepth());
            if (domainNode.getPosition() != null) {
                entity.setPosition(domainNode.getPosition().getValue());
            }
            entity.setUpdatedAt(LocalDateTime.now());
            if (parentEntity != null) {
                entity.setParent(parentEntity);
            }
            
            // Sync tags
            syncTags(domainNode, entity);
            
            // Recursively persist children
            for (Node child : domainNode.getChildren()) {
                persistRecursively(child, entity);
            }
        } else {
            // Entity doesn't exist - this shouldn't happen in normal operations
            // but we'll handle it by using the mapper to create a new entity
            NodeJpaEntity mapped = mapper.toJpaEntity(domainNode);
            if (parentEntity != null) {
                mapped.setParent(parentEntity);
            }
            NodeJpaEntity saved = jpaRepository.save(mapped);
            
            // Sync tags
            syncTags(domainNode, saved);
            
            // Recursively persist children
            for (Node child : domainNode.getChildren()) {
                persistRecursively(child, saved);
            }
        }
    }
    
    private void syncTags(Node domainNode, NodeJpaEntity entity) {
        // Get existing tags from database
        List<TagJpaEntity> existingTags = jpaTagRepository.findByNodeId(entity.getId());
        Map<String, TagJpaEntity> existingTagMap = existingTags.stream()
            .collect(Collectors.toMap(TagJpaEntity::getTagKey, t -> t));
        
        // Get current tags from domain
        Map<TagKey, Tag> domainTags = domainNode.getTags();
        
        // Add or update tags from domain
        for (Map.Entry<TagKey, Tag> entry : domainTags.entrySet()) {
            String key = entry.getKey().getValue();
            String value = entry.getValue().getValue().toString();
            
            TagJpaEntity existingTag = existingTagMap.get(key);
            if (existingTag != null) {
                // Update existing tag
                existingTag.setTagValue(value);
                existingTagMap.remove(key);
            } else {
                // Create new tag
                TagJpaEntity newTag = new TagJpaEntity();
                newTag.setNode(entity);
                newTag.setTagKey(key);
                newTag.setTagValue(value);
                newTag.setCreatedAt(LocalDateTime.now());
                jpaTagRepository.save(newTag);
            }
        }
        
        // Delete tags that no longer exist in domain
        for (TagJpaEntity tagToDelete : existingTagMap.values()) {
            jpaTagRepository.delete(tagToDelete);
        }
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
