-- =============================================================================
-- V18: Add label tables for Reports and initial labels for Report Definitions
-- Report labels are user-editable tags for categorization and filtering.
-- Report definition initial labels are copied to new reports on creation.
-- =============================================================================

CREATE TABLE IF NOT EXISTS report_label (
    report_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    UNIQUE (report_id, label),
    FOREIGN KEY (report_id) REFERENCES report(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_report_label ON report_label(label, report_id);

CREATE TABLE IF NOT EXISTS report_definition_initial_label (
    report_definition_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    UNIQUE (report_definition_id, label),
    FOREIGN KEY (report_definition_id) REFERENCES report_definition(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_report_definition_initial_label ON report_definition_initial_label(label, report_definition_id);
