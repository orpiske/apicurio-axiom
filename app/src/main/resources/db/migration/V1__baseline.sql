-- =============================================================================
-- V1: Baseline schema for Apicurio Axiom
-- This migration creates the complete schema for existing persistent databases.
-- Flyway's baseline-on-migrate handles databases that already have these tables.
-- =============================================================================

CREATE TABLE IF NOT EXISTS action_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    execution_mode VARCHAR(255) NOT NULL,
    user_triggerable BOOLEAN NOT NULL DEFAULT FALSE,
    input_schema TEXT,
    allowed_tools TEXT,
    prompt_template TEXT,
    script_template TEXT,
    model VARCHAR(255),
    emits_event BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    task_id BIGINT,
    event_id BIGINT,
    entry_type VARCHAR(255) NOT NULL,
    summary VARCHAR(1024) NOT NULL,
    details TEXT,
    created_on TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS actor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    capabilities TEXT,
    permissions TEXT,
    configuration TEXT
);

CREATE TABLE IF NOT EXISTS ai_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invocation_type VARCHAR(255) NOT NULL,
    task_id BIGINT,
    event_id BIGINT,
    project_id BIGINT,
    actor_id BIGINT,
    action_type VARCHAR(255),
    model VARCHAR(255),
    cost_usd DOUBLE,
    input_tokens BIGINT,
    output_tokens BIGINT,
    duration_ms BIGINT,
    created_on TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    issue_ref VARCHAR(255),
    repository VARCHAR(255),
    project_id BIGINT,
    task_id BIGINT,
    payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS event_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    enqueued_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manager_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_prompt TEXT,
    prompt_template TEXT
);

CREATE TABLE IF NOT EXISTS mcp_server (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    server_command VARCHAR(255),
    server_args TEXT,
    server_env TEXT,
    server_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    issue_source VARCHAR(255) NOT NULL,
    issue_ref VARCHAR(255) NOT NULL UNIQUE,
    repository VARCHAR(255) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    updated_on TIMESTAMP NOT NULL,
    metadata TEXT,
    disk_usage_bytes BIGINT
);

CREATE TABLE IF NOT EXISTS report_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    schedule VARCHAR(255) NOT NULL,
    schedule_time VARCHAR(255),
    time_window VARCHAR(255) NOT NULL,
    prompt_template TEXT NOT NULL,
    allowed_tools TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    next_run_at TIMESTAMP,
    last_run_at TIMESTAMP,
    created_on TIMESTAMP NOT NULL,
    updated_on TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS report_definition_repositories (
    report_definition_entity_id BIGINT NOT NULL,
    repositories VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    content TEXT,
    time_range_start TIMESTAMP,
    time_range_end TIMESTAMP,
    execution_log TEXT,
    cost_usd DOUBLE,
    created_on TIMESTAMP NOT NULL,
    completed_on TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repository (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    poll_interval INTEGER,
    webhook_secret VARCHAR(255),
    configuration TEXT,
    polling_enabled BOOLEAN DEFAULT FALSE,
    last_polled_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    action_type VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    event_id BIGINT,
    assigned_actor BIGINT,
    status VARCHAR(255) NOT NULL,
    input TEXT,
    output TEXT,
    created_on TIMESTAMP NOT NULL,
    completed_on TIMESTAMP,
    session_id VARCHAR(255),
    execution_log TEXT
);

CREATE TABLE IF NOT EXISTS thread_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    author_type VARCHAR(255) NOT NULL,
    author_id VARCHAR(255),
    entry_type VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_on TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tool_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    parameters TEXT,
    script_template TEXT
);

CREATE TABLE IF NOT EXISTS toolset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    tools TEXT NOT NULL
);
