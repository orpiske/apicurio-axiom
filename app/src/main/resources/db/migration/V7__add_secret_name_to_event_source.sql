-- =============================================================================
-- V7: Add secret_name column to event_source table
-- Allows per-source authentication secret override.
-- =============================================================================

ALTER TABLE event_source ADD COLUMN IF NOT EXISTS secret_name VARCHAR(255);
