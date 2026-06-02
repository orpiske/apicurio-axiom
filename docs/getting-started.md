# Getting Started

The fastest way to get started with Apitomy Axiom is to use the `run-axiom.sh` script, which
downloads and runs the latest release automatically. If you want to build from source for
development, see [Building from Source](#building-from-source) below.

## Quick Start

### Prerequisites

- **Java** 25+
- **curl** and **jq** (for downloading releases)
- One of the supported AI engine CLIs:

| Engine | Config Value | Binary | Install |
|--------|-------------|--------|---------|
| Claude Code | `claude-code` (default) | `claude` | `npm install -g @anthropic-ai/claude-code` |
| OpenCode | `opencode` | `opencode` | `curl -fsSL https://opencode.ai/install \| bash` |

- An API key for your LLM provider (e.g., `ANTHROPIC_API_KEY` environment variable)

### Run Axiom

Download and run the script:

```bash
curl -fsSL https://raw.githubusercontent.com/Apitomy/apitomy-axiom/main/scripts/run-axiom.sh | bash
```

Or clone the repo and run it directly:

```bash
git clone https://github.com/Apitomy/apitomy-axiom.git
cd apitomy-axiom
./scripts/run-axiom.sh
```

The script will:

1. Query GitHub for the latest Apitomy Axiom release
2. Download the application JAR (cached in `~/.axiom/releases/` for future runs)
3. Start the application on **http://localhost:9191**

Open **http://localhost:9191** in your browser to access the Axiom dashboard.

### Setting Up an Event Source

Once Axiom is running, configure a GitHub event source through the UI:

1. Open **http://localhost:9191**
2. Go to **Event Sources** → **Add Event Source**
3. Enter the GitHub repository (e.g., `Apitomy/apitomy-data-models`)
4. Provide a GitHub personal access token with `repo` scope
5. Enable polling (default interval: 60 seconds)

Axiom will start monitoring the repository for new issues, pull requests, and comments.

---

## Building from Source

If you want to contribute or run in development mode, you'll need to build from source.

### Additional Prerequisites

- **Maven** 3.9+
- **Node.js** 20+ (for the UI)

### Building

```bash
# Backend only (no UI)
./build.sh

# Full release build (backend + UI bundled)
./build-release.sh
```

### Running in Development Mode

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

To run the backend only (no UI):

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

## Next Steps

- [User Guide](user-guide/index.md) — Architecture details, extending Axiom, and advanced
  configuration
