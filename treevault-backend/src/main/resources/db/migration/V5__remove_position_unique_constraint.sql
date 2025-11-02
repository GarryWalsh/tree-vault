-- V5__remove_position_unique_constraint.sql
-- Remove unique constraint on position to allow flexible reordering
-- Application logic will ensure consistency (following industry best practices like Jira, Trello)
ALTER TABLE nodes DROP CONSTRAINT IF EXISTS uk_parent_position;


