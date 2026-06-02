# User Guide

This guide covers the architecture, extensibility, and configuration of Apitomy Axiom.

!!! note "Under Development"
    This user guide is under active development. New sections will be added over time.

## Table of Contents

- [Architecture](#architecture)
- [Event Pipeline](#event-pipeline)
- [AI Manager](#ai-manager)
- [Actors](#actors)
- [Project Lifecycle](#project-lifecycle)
- [Database Migrations](#database-migrations)

---

## Architecture

Axiom is built as a multi-module Quarkus application with clear separation of concerns:

| Module | Purpose |
|--------|---------|
| `common/api` | OpenAPI contract and generated JAX-RS interfaces |
| `core` | Domain entities, lifecycle state machine, workspace management |
| `engine/spi` | Pluggable AI engine abstraction (interface + registry) |
| `engine/opencode` | OpenCode engine implementation |
| `manager` | AI Manager — event triage and decision-making |
| `actors/spi` | Actor interface and context |
| `actors/claude-code` | Claude Code CLI subprocess actor + engine |
| `actors/human` | Human actor (notification-driven) |
| `events/core` | Event queue and pipeline orchestrator |
| `events/github` | GitHub webhooks and API polling |
| `events/jira` | Jira integration |
| `notifications/spi` | Notification channel interface |
| `notifications/slack` | Slack notifications |
| `notifications/telegram` | Telegram notifications |
| `app` | Quarkus application (assembles all modules) |
| `ui` | React frontend |
| `ui-bundle` | Bundles UI assets into backend JAR |

### API-First Design

The REST API is defined in an OpenAPI specification at `common/api/src/main/resources/axiom.yaml`.
JAX-RS interfaces are generated at build time using the
[Apitomy Codegen](https://www.apitomy.io/projects/codegen/) plugin. Resource implementations in
the `app` module implement these generated interfaces.

---

## Event Pipeline

Events flow through a pipeline:

1. **Event Sources** poll external systems (GitHub, Jira) for activity
2. New events are normalized and placed in an **Event Queue**
3. The **Pipeline Orchestrator** dequeues events and passes them to the **AI Manager**
4. The Manager produces a **Decision** (create project, assign task, ignore, etc.)
5. The orchestrator executes the decision

### Event Sources

Event sources are configured through the UI or API. Each source polls a specific repository
at a configurable interval. Supported sources:

- **GitHub** — issues, pull requests, comments, reviews
- **Jira** — issues and comments (planned)

---

## AI Manager

The Manager receives events and produces structured decisions. It uses the configured AI
engine to analyze the event context and determine the appropriate action.

### Decision Types

| Decision | Description |
|----------|-------------|
| `create_project` | Create a new project for an issue |
| `assign_task` | Assign a task to an actor |
| `update_project` | Update an existing project's status |
| `ignore` | No action needed for this event |
| `request_info` | Need more information before deciding |

### Confidence Threshold

The Manager includes a confidence score (0.0 - 1.0) with each decision. Only decisions above
the configured threshold (`axiom.manager.confidence-threshold`, default 0.7) are auto-executed.
Lower-confidence decisions are flagged for human review in the UI.

---

## Actors

Actors execute tasks assigned by the Manager. Each actor implementation handles a specific
type of work.

### Claude Code Actor

The default actor uses the Claude Code CLI as a subprocess. It:

- Creates a workspace directory for each project
- Runs Claude Code with the task prompt and configured MCP tools
- Captures the execution output and result
- Reports back to the orchestrator

### OpenCode Actor

An alternative actor using the OpenCode CLI. Supports the same task execution model with
a different underlying AI engine.

### Human Actor

Sends notifications (Slack, Telegram) to human team members. The task remains in a pending
state until the human completes it and updates the status through the UI.

---

## Project Lifecycle

Projects follow a state machine:

```
CREATED → IN_PROGRESS → COMPLETED
                      → FAILED
         → BLOCKED    → IN_PROGRESS
         → CANCELLED
```

State transitions are enforced by the `ProjectLifecycle` class. Invalid transitions throw
`InvalidStatusTransitionException`.

---

## Database Migrations

Schema changes are managed by [Flyway](https://flywaydb.org/) and run automatically on
startup for persistent databases.

### Adding a Migration

1. Create a SQL file in `app/src/main/resources/db/migration/`
2. Follow the naming convention: `V<number>__<description>.sql`
3. Use `IF NOT EXISTS` / `IF EXISTS` guards for safety

### Example

```sql
-- V3__add_priority_to_task.sql
ALTER TABLE task ADD COLUMN IF NOT EXISTS priority VARCHAR(255) DEFAULT 'normal';
```

Flyway is configured with `baseline-on-migrate=true` and `baseline-version=1`, so existing
databases are automatically baselined.
