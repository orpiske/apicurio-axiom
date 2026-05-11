-- =============================================================================
-- V10: Add event_source_id column to event table
-- Tracks which Event Source produced each event. Nullable for events not
-- originating from an Event Source (e.g. internal events).
-- =============================================================================

ALTER TABLE event ADD COLUMN IF NOT EXISTS event_source_id BIGINT;
