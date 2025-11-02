package com.treevault.infrastructure.persistence.adapter;

import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import com.treevault.domain.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class NodeRepositoryAdapterTest {
    
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16");

    @Container
    private static final PostgreSQLContainer<?> postgres = createPostgresContainer();

    private static PostgreSQLContainer<?> createPostgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_IMAGE);
        container.withDatabaseName("treevault_test");
        container.withUsername("test");
        container.withPassword("test");
        return container;
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }
    
    @Autowired
    private NodeRepository adapter;
    
    @Test
    @DisplayName("Should save and retrieve node successfully")
    void shouldSaveAndRetrieveNodeSuccessfully() {
        // Given
        Node node = Node.createFolder(NodeName.of("TestFolder"), null);
        
        // When
        Node saved = adapter.save(node);
        Node retrieved = adapter.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(saved.getId());
        assertThat(retrieved.getName().getValue()).isEqualTo("TestFolder");
        assertThat(retrieved.getType()).isEqualTo(NodeType.FOLDER);
    }
    
    @Test
    @DisplayName("Should save node with tags")
    void shouldSaveNodeWithTags() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        node.addTag(TagKey.of("department"), TagValue.of("engineering"));
        node.addTag(TagKey.of("status"), TagValue.of("active"));
        
        // When
        Node saved = adapter.save(node);
        Node retrieved = adapter.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved.getTags()).hasSize(2);
        assertThat(retrieved.getTags()).containsKey(TagKey.of("department"));
        assertThat(retrieved.getTags()).containsKey(TagKey.of("status"));
    }
    
    @Test
    @DisplayName("Should save and retrieve tree structure")
    void shouldSaveAndRetrieveTreeStructure() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file1 = Node.createFile(NodeName.of("file1.txt"), folder1);
        Node file2 = Node.createFile(NodeName.of("file2.txt"), folder1);
        
        // When
        Node savedRoot = adapter.save(root);
        Node retrieved = adapter.findById(savedRoot.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved.getChildren()).hasSize(2);
        assertThat(retrieved.getChildren().get(0).getName().getValue()).isEqualTo("Folder1");
        assertThat(retrieved.getChildren().get(0).getChildren()).hasSize(2);
    }
    
    @Test
    @DisplayName("Should delete node and cascade children")
    void shouldDeleteNodeAndCascadeChildren() {
        // Given
        Node root = Node.createRoot();
        Node folder = Node.createFolder(NodeName.of("Folder"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder);
        
        Node savedRoot = adapter.save(root);
        Node savedFolder = savedRoot.getChildren().get(0);
        
        // When
        adapter.delete(savedFolder);
        
        // Then
        assertThat(adapter.findById(savedFolder.getId())).isEmpty();
    }
    
    @Test
    @DisplayName("Should find root node")
    void shouldFindRootNode() {
        // Given
        Node root = Node.createRoot();
        adapter.save(root);
        
        // When
        java.util.Optional<Node> found = adapter.findRootNode();
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().isRoot()).isTrue();
    }
    
    @Test
    @DisplayName("Should find nodes by parent ID")
    void shouldFindNodesByParentId() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node file = Node.createFile(NodeName.of("file.txt"), folder1);
        
        Node savedRoot = adapter.save(root);
        
        // When
        java.util.List<Node> children = adapter.findByParentId(savedRoot.getId());
        
        // Then
        assertThat(children).hasSize(2);
        assertThat(children).extracting(node -> node.getName().getValue())
            .containsExactlyInAnyOrder("Folder1", "Folder2");
    }
    
    @Test
    @DisplayName("Should check existence by parent and name")
    void shouldCheckExistenceByParentAndName() {
        // Given
        Node root = Node.createRoot();
        Node folder = Node.createFolder(NodeName.of("Folder"), root);
        adapter.save(root);
        Node savedFolder = adapter.findByParentId(root.getId()).get(0);
        
        // When
        boolean exists = adapter.existsByParentAndName(root.getId(), NodeName.of("Folder"));
        boolean notExists = adapter.existsByParentAndName(root.getId(), NodeName.of("NonExistent"));
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("Should update tags when node is saved")
    void shouldUpdateTagsWhenNodeIsSaved() {
        // Given
        Node node = Node.createFolder(NodeName.of("Folder"), null);
        node.addTag(TagKey.of("old"), TagValue.of("value1"));
        Node saved = adapter.save(node);
        
        // When - Update tags
        saved.addTag(TagKey.of("old"), TagValue.of("value2")); // Overwrite
        saved.addTag(TagKey.of("new"), TagValue.of("value3")); // Add new
        Node updated = adapter.save(saved);
        
        // Then
        assertThat(updated.getTags()).hasSize(2);
        assertThat(updated.getTags().get(TagKey.of("old")).getValue().getValue()).isEqualTo("value2");
        assertThat(updated.getTags().get(TagKey.of("new")).getValue().getValue()).isEqualTo("value3");
    }
    
    @Test
    @DisplayName("Should handle deep tree reconstruction")
    void shouldHandleDeepTreeReconstruction() {
        // Given - Create deep tree
        Node current = Node.createRoot();
        Node root = current;
        for (int i = 0; i < 10; i++) {
            current = Node.createFolder(NodeName.of("level" + i), current);
        }
        
        // When
        Node savedRoot = adapter.save(root);
        Node retrieved = adapter.findById(savedRoot.getId()).orElseThrow();
        
        // Then - Verify deep structure is preserved
        Node deepest = retrieved;
        int depth = 0;
        while (!deepest.getChildren().isEmpty()) {
            deepest = deepest.getChildren().get(0);
            depth++;
        }
        assertThat(depth).isEqualTo(10);
    }
    
    @Test
    @DisplayName("Should return empty when node not found")
    void shouldReturnEmptyWhenNodeNotFound() {
        // Given
        NodeId nonExistentId = NodeId.generate();
        
        // When
        java.util.Optional<Node> result = adapter.findById(nonExistentId);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should preserve position ordering")
    void shouldPreservePositionOrdering() {
        // Given
        Node root = Node.createRoot();
        Node folder1 = Node.createFolder(NodeName.of("Folder1"), root);
        Node folder2 = Node.createFolder(NodeName.of("Folder2"), root);
        Node folder3 = Node.createFolder(NodeName.of("Folder3"), root);
        
        // When
        Node savedRoot = adapter.save(root);
        java.util.List<Node> children = adapter.findByParentId(savedRoot.getId());
        
        // Then
        assertThat(children).hasSize(3);
        assertThat(children.get(0).getPosition().getValue()).isEqualTo(0);
        assertThat(children.get(1).getPosition().getValue()).isEqualTo(1);
        assertThat(children.get(2).getPosition().getValue()).isEqualTo(2);
    }
}

