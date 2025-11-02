-- V3__create_indexes.sql
CREATE INDEX idx_nodes_parent ON nodes(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_nodes_path_pattern ON nodes(path text_pattern_ops);
CREATE INDEX idx_nodes_type ON nodes(type);
CREATE INDEX idx_nodes_created_at ON nodes(created_at);
CREATE INDEX idx_tags_node ON tags(node_id);
CREATE INDEX idx_tags_key ON tags(tag_key);

