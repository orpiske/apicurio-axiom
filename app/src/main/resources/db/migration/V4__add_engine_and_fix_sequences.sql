-- =============================================================================
-- V4: Add engine column to action_type and create missing sequences
-- =============================================================================

-- Per-action-type AI engine override (e.g. "opencode", "claude-code")
ALTER TABLE action_type ADD COLUMN IF NOT EXISTS engine VARCHAR(255);

-- Hibernate uses sequence-based ID generation for Panache entities.
-- Create sequences for any tables that were created with AUTO_INCREMENT
-- but need a sequence for Hibernate compatibility.
CREATE SEQUENCE IF NOT EXISTS secret_SEQ START WITH 1 INCREMENT BY 50;
