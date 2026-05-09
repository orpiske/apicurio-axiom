-- =============================================================================
-- V9: Add environment column to action_type and report_definition
-- Allows per-action-type and per-report environment variable configuration.
-- =============================================================================

ALTER TABLE action_type ADD COLUMN IF NOT EXISTS environment TEXT;
ALTER TABLE report_definition ADD COLUMN IF NOT EXISTS environment TEXT;
