package com.treevault.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nodes", indexes = {
    @Index(name = "idx_nodes_parent", columnList = "parent_id"),
    @Index(name = "idx_nodes_path_pattern", columnList = "path"),
    @Index(name = "idx_nodes_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeJpaEntity {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NodeType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NodeJpaEntity parent;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;
    
    @Column(nullable = false)
    private Integer depth;
    
    @Column(nullable = false)
    private Integer position;
    
    @Version
    @Column(nullable = false)
    private Long version;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum NodeType {
        FOLDER, FILE
    }
}

