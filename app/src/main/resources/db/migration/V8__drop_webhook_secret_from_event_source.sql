-- =============================================================================
-- V8: Remove webhook_secret column from event_source table
-- Webhook support has been removed in favor of polling-only event sources.
-- =============================================================================

ALTER TABLE event_source DROP COLUMN IF EXISTS webhook_secret;
