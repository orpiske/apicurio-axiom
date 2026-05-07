-- =============================================================================
-- V3: Create secret table for encrypted environment variable storage
-- Secrets are injected into actor subprocess environments at execution time.
-- =============================================================================

CREATE TABLE IF NOT EXISTS secret (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    encrypted_value TEXT NOT NULL
);
