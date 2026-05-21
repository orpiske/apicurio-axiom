-- =============================================================================
-- V17: Add label tables for Tools and Projects
-- Labels are free-form strings for categorization and filtering.
-- =============================================================================

CREATE TABLE IF NOT EXISTS tool_label (
    tool_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    UNIQUE (tool_id, label),
    FOREIGN KEY (tool_id) REFERENCES tool_definition(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_tool_label ON tool_label(label, tool_id);

CREATE TABLE IF NOT EXISTS project_label (
    project_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    UNIQUE (project_id, label),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_project_label ON project_label(label, project_id);
