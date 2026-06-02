# Getting Started

This guide walks you through building and running Apitomy Axiom locally.

## Prerequisites

- **Java** 25+
- **Maven** 3.9+
- **Node.js** 20+ (for the UI and MCP tool server)
- One of the supported AI engine CLIs:

| Engine | Config Value | Binary | Install |
|--------|-------------|--------|---------|
| Claude Code | `claude-code` (default) | `claude` | `npm install -g @anthropic-ai/claude-code` |
| OpenCode | `opencode` | `opencode` | `curl -fsSL https://opencode.ai/install \| bash` |

- An API key for your LLM provider (e.g., `ANTHROPIC_API_KEY` environment variable)

## Building

### Backend only (no UI)

```bash
./build.sh
```

### Full release build (backend + UI bundled)

```bash
./build-release.sh
```

## Running in Development Mode

The `dev.sh` script starts both the Quarkus backend and the Vite UI dev server:

```bash
./dev.sh
```

This will:

1. Build all Maven modules
2. Install UI dependencies (first run only)
3. Start the Quarkus backend on **http://localhost:8080**
4. Start the Vite UI dev server on **http://localhost:8888**

Open **http://localhost:8888** in your browser.

### Backend only (no UI)

```bash
./dev.sh --skip-ui
```

## Configuration

Configuration is managed through `app/src/main/resources/application.properties`.

### Key Settings

| Property | Default | Description |
|----------|---------|-------------|
| `axiom.ai-engine` | `claude-code` | AI engine to use (`claude-code` or `opencode`) |
| `axiom.manager.confidence-threshold` | `0.7` | Minimum confidence for auto-execution |
| `axiom.workspace.root` | `~/.axiom/workspaces` | Root directory for project workspaces |

### Database Profiles

| Profile | Database | Usage |
|---------|----------|-------|
| `dev` (default) | H2 in-memory | Development — schema recreated on restart |
| `persist` | H2 file (`~/.axiom/data/axiom`) | Persistent dev — Flyway migrations |
| `prod` | H2 file | Production — uber-jar with Flyway |

To use the persistent profile:

```bash
mvn quarkus:dev -Dquarkus.profile=persist
```

## Setting Up an Event Source

Once Axiom is running, configure a GitHub event source through the UI:

1. Open **http://localhost:8888**
2. Go to **Event Sources** → **Add Event Source**
3. Enter the GitHub repository (e.g., `Apitomy/apitomy-data-models`)
4. Provide a GitHub personal access token with `repo` scope
5. Enable polling (default interval: 60 seconds)

Axiom will start monitoring the repository for new issues, pull requests, and comments.

## Next Steps

- [User Guide](user-guide/index.md) — Architecture details, extending Axiom, and advanced
  configuration
