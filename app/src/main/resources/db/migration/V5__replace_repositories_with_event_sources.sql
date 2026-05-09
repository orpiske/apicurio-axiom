-- =============================================================================
-- V5: Replace repositories with event sources
-- Event sources are a generic, extensible model for monitoring GitHub repos,
-- Jira projects, and other event providers.
-- =============================================================================

CREATE TABLE IF NOT EXISTS event_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    poll_interval INTEGER,
    last_polled_at TIMESTAMP,
    webhook_secret VARCHAR(255),
    configuration TEXT NOT NULL
);

-- Drop the old repository-related tables
DROP TABLE IF EXISTS report_definition_repositories;
DROP TABLE IF EXISTS repository;
