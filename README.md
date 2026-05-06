# Apicurio Axiom

Event-driven project orchestration platform that monitors GitHub and Jira issues, creates
long-lived projects around them, and delegates work to human and AI actors.

## What It Does

- Watches GitHub/Jira repositories for issue activity (webhooks or polling)
- An AI Manager triages events and decides what actions to take
- Tasks are assigned to Actors (AI agents or humans)
- Projects track the full lifecycle of work tied to each issue
- A UI provides visibility into projects, tasks, activity, and configuration

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Java 25 / Quarkus 3.33 LTS |
| Frontend | TypeScript / React / PatternFly |
| Database | H2 (dev) / PostgreSQL (prod) |
| AI | Pluggable: Claude Code CLI or OpenCode (configurable) |
| API | Contract-first OpenAPI + apicurio-codegen |

## AI Engine

Axiom's AI engine is pluggable. Set `axiom.ai-engine` in `application.properties` to
select which engine to use:

| Engine | Config Value | Binary | Install |
|--------|-------------|--------|---------|
| Claude Code | `claude-code` (default) | `claude` | `npm install -g @anthropic-ai/claude-code` |
| OpenCode | `opencode` | `opencode` | `curl -fsSL https://opencode.ai/install \| bash` |

Only the selected engine's binary needs to be installed. Both can coexist for testing.

## Prerequisites

- Java 25+
- Maven 3.9+
- Node.js 20+ (for UI and MCP tool server)
- One of the AI engine CLIs (see table above)
- API key for your LLM provider (e.g. `ANTHROPIC_API_KEY`)

## Build

```bash
# Standard build (skips Claude CLI integration tests)
./build.sh

# Full build including Claude CLI integration tests
./build-all.sh
```

## Development

```bash
# Backend (Quarkus dev mode with H2)
cd app && mvn quarkus:dev

# Frontend (Vite dev server, proxies API to localhost:8080)
cd ui && npm install && npm run dev
```

## Project Structure

```
common/api/          OpenAPI contract + generated JAX-RS interfaces
core/                Domain entities, lifecycle, workspace management
engine/spi/          Pluggable AI engine abstraction layer
engine/opencode/     OpenCode engine implementation
manager/             AI Manager (event triage and decision-making)
actors/spi/          Actor interface
actors/claude-code/  Claude Code CLI subprocess actor + engine
actors/human/        Human actor (notification-driven)
events/core/         Event queue and pipeline orchestrator
events/github/       GitHub webhooks + API polling
events/jira/         Jira integration (planned)
notifications/       Slack, Telegram channels (planned)
app/                 Quarkus application (assembles everything)
ui/                  React frontend
```

## Documentation

- [Functional Design](docs/design-v2.md)
- [Architecture](docs/architecture-v2.md)
- [Implementation Plan](docs/implementation-plan.md)
- [OpenCode Migration Plan](docs/opencode-migration-plan.md)
- [Claude Code Integration Research](docs/research-claude-code-integration.md)

## License

[Apache License 2.0](LICENSE)
