-- V1__create_nodes_table.sql
CREATE TABLE nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FOLDER', 'FILE')),
    parent_id UUID REFERENCES nodes(id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    depth INTEGER NOT NULL DEFAULT 0,
    position INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add constraints
ALTER TABLE nodes ADD CONSTRAINT uk_parent_position 
    UNIQUE(parent_id, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE nodes ADD CONSTRAINT uk_parent_name 
    UNIQUE(parent_id, name);

