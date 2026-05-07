-- =============================================================================
-- V2: Add manager_triggerable column to action_type table
-- Controls whether the AI Manager can select this action type during triage.
-- Defaults to TRUE so existing action types remain visible to the Manager.
-- =============================================================================

ALTER TABLE action_type ADD COLUMN IF NOT EXISTS manager_triggerable BOOLEAN NOT NULL DEFAULT TRUE;
