const { Server } = require("@modelcontextprotocol/sdk/server/index.js");
const { StdioServerTransport } = require("@modelcontextprotocol/sdk/server/stdio.js");
const { ListToolsRequestSchema, CallToolRequestSchema } = require("@modelcontextprotocol/sdk/types.js");
const { execSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

// Logging helper — writes to stderr so it doesn't interfere with MCP stdio protocol
function log(level, message, data) {
    const entry = {
        timestamp: new Date().toISOString(),
        level,
        source: "axiom-mcp-server",
        message,
        ...data
    };
    process.stderr.write(JSON.stringify(entry) + "\n");
}

// Axiom REST API base URL (injected via environment)
const AXIOM_API_URL = process.env.AXIOM_API_URL || "http://localhost:8080/api/v1";

// Helper for Axiom API calls
async function axiomApi(method, path, body) {
    const url = `${AXIOM_API_URL}${path}`;
    const opts = {
        method,
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
    };
    if (body) opts.body = JSON.stringify(body);
    const resp = await fetch(url, opts);
    const text = await resp.text();
    if (!resp.ok) {
        throw new Error(`Axiom API ${method} ${path} returned ${resp.status}: ${text.substring(0, 500)}`);
    }
    return text;
}

// ── Built-in Axiom SDK tools ─────────────────────────────────────
const SDK_TOOLS = [
    {
        name: "axiom_fire_event",
        description: "Fire a new event into Axiom for processing by the Manager. The event will be evaluated and may trigger project creation and task execution.",
        parameters: [
            { name: "source", type: "string", description: "Event source type (e.g. 'github', 'jira', 'internal')", required: true },
            { name: "eventType", type: "string", description: "Event type (e.g. 'issue-created', 'custom')", required: true },
            { name: "issueRef", type: "string", description: "Issue reference (e.g. 'owner/repo#123')", required: false },
            { name: "repository", type: "string", description: "Repository identifier (e.g. 'owner/repo')", required: false },
            { name: "payload", type: "string", description: "JSON payload with event details", required: true },
        ],
        handler: async (args) => {
            return await axiomApi("POST", "/events", {
                source: args.source,
                eventType: args.eventType,
                issueRef: args.issueRef || null,
                repository: args.repository || null,
                payload: args.payload,
            });
        },
    },
    {
        name: "axiom_list_projects",
        description: "List existing Axiom projects with optional filtering by name and status.",
        parameters: [
            { name: "filterName", type: "string", description: "Filter by project name or issue ref (substring match)", required: false },
            { name: "filterStatus", type: "string", description: "Filter by status: Created, InProgress, Idle, Completed (comma-separated for multiple)", required: false },
        ],
        handler: async (args) => {
            const params = new URLSearchParams();
            params.set("limit", "50");
            if (args.filterName) params.set("filterName", args.filterName);
            if (args.filterStatus) params.set("filterStatus", args.filterStatus);
            return await axiomApi("GET", `/projects?${params}`);
        },
    },
    {
        name: "axiom_get_project",
        description: "Get details of a specific Axiom project including metadata, status, and issue reference.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID", required: true },
        ],
        handler: async (args) => {
            return await axiomApi("GET", `/projects/${args.projectId}`);
        },
    },
    {
        name: "axiom_create_task",
        description: "Create a new task on an Axiom project. The task will be queued and executed by an available actor.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID to create the task on", required: true },
            { name: "actionType", type: "string", description: "The action type name (e.g. 'analyze', 'implement')", required: true },
            { name: "input", type: "string", description: "Input context or instructions for the task", required: false },
        ],
        handler: async (args) => {
            return await axiomApi("POST", `/projects/${args.projectId}/tasks`, {
                actionType: args.actionType,
                input: args.input || null,
            });
        },
    },
    {
        name: "axiom_get_task_status",
        description: "Get the status and details of a specific task on a project.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID", required: true },
            { name: "taskId", type: "number", description: "The task ID", required: true },
        ],
        handler: async (args) => {
            // Fetch all tasks for the project and find the specific one
            const result = await axiomApi("GET", `/tasks?filterProjectId=${args.projectId}&limit=100`);
            const parsed = JSON.parse(result);
            const task = (parsed.items || []).find(t => t.id === Number(args.taskId));
            return task ? JSON.stringify(task, null, 2) : `Task #${args.taskId} not found in project #${args.projectId}`;
        },
    },
    {
        name: "axiom_add_thread_entry",
        description: "Post an update or message to a project's conversation thread. Useful for logging progress or sharing findings with other agents.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID", required: true },
            { name: "content", type: "string", description: "The message content to post", required: true },
        ],
        handler: async (args) => {
            return await axiomApi("POST", `/projects/${args.projectId}/thread`, {
                content: args.content,
            });
        },
    },
    {
        name: "axiom_close_project",
        description: "Close (complete) an Axiom project. The project must be in an active state.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID to close", required: true },
        ],
        handler: async (args) => {
            return await axiomApi("POST", `/projects/${args.projectId}/close`);
        },
    },
    {
        name: "axiom_reopen_project",
        description: "Reopen a previously closed Axiom project.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID to reopen", required: true },
        ],
        handler: async (args) => {
            return await axiomApi("POST", `/projects/${args.projectId}/reopen`);
        },
    },
    {
        name: "axiom_add_project_label",
        description: "Add a label to an Axiom project for categorization and filtering.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID", required: true },
            { name: "label", type: "string", description: "The label to add", required: true },
        ],
        handler: async (args) => {
            const project = JSON.parse(await axiomApi("GET", `/projects/${args.projectId}`));
            const labels = project.labels || [];
            if (!labels.includes(args.label)) {
                labels.push(args.label);
                return await axiomApi("PUT", `/projects/${args.projectId}`, { labels });
            }
            return JSON.stringify(project);
        },
    },
    {
        name: "axiom_remove_project_label",
        description: "Remove a label from an Axiom project.",
        parameters: [
            { name: "projectId", type: "number", description: "The project ID", required: true },
            { name: "label", type: "string", description: "The label to remove", required: true },
        ],
        handler: async (args) => {
            const project = JSON.parse(await axiomApi("GET", `/projects/${args.projectId}`));
            const labels = (project.labels || []).filter(l => l !== args.label);
            return await axiomApi("PUT", `/projects/${args.projectId}`, { labels });
        },
    },
];

// ── Script-based tools (loaded from JSON file) ──────────────────
const toolsFile = process.argv[2];
if (!toolsFile) {
    log("ERROR", "Usage: node server.js <tools.json>");
    process.exit(1);
}
const SCRIPT_TOOLS = JSON.parse(fs.readFileSync(toolsFile, "utf-8"));

// Merge all tools
const ALL_TOOLS = [...SDK_TOOLS, ...SCRIPT_TOOLS];
log("INFO", "Axiom MCP server started", {
    sdkTools: SDK_TOOLS.length,
    scriptTools: SCRIPT_TOOLS.length,
    totalTools: ALL_TOOLS.length,
    toolsFile,
    axiomApiUrl: AXIOM_API_URL,
});

const server = new Server({ name: "axiom-tools", version: "1.0.0" }, {
    capabilities: { tools: {} }
});

server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: ALL_TOOLS.map(t => ({
        name: t.name,
        description: t.description || "",
        inputSchema: {
            type: "object",
            properties: Object.fromEntries(
                (t.parameters || []).map(p => [p.name, {
                    type: p.type || "string",
                    description: p.description || ""
                }])
            ),
            required: (t.parameters || []).filter(p => p.required).map(p => p.name)
        }
    }))
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const toolName = request.params.name;
    const args = request.params.arguments || {};

    // Check SDK tools first
    const sdkTool = SDK_TOOLS.find(t => t.name === toolName);
    if (sdkTool) {
        log("INFO", "SDK tool called", { toolName, args: Object.keys(args) });
        try {
            const startTime = Date.now();
            const result = await sdkTool.handler(args);
            const durationMs = Date.now() - startTime;
            log("INFO", "SDK tool completed", { toolName, durationMs });
            return { content: [{ type: "text", text: result || "OK" }] };
        } catch (error) {
            log("ERROR", "SDK tool failed", { toolName, error: error.message });
            return { content: [{ type: "text", text: error.message }], isError: true };
        }
    }

    // Check script tools
    const scriptTool = SCRIPT_TOOLS.find(t => t.name === toolName);
    if (!scriptTool) {
        log("WARN", "Unknown tool called", { toolName });
        return { content: [{ type: "text", text: "Unknown tool: " + toolName }], isError: true };
    }

    log("INFO", "Script tool called", { toolName, args: Object.keys(args) });

    try {
        let cmd = scriptTool.scriptTemplate;
        for (const [key, value] of Object.entries(args)) {
            const fileKey = "{{" + key + "_file}}";
            if (cmd.includes(fileKey)) {
                const tmpFile = path.join(os.tmpdir(), `axiom-tool-${toolName}-${key}-${Date.now()}.txt`);
                fs.writeFileSync(tmpFile, String(value));
                cmd = cmd.replaceAll(fileKey, tmpFile);
            }
            cmd = cmd.replaceAll("{{" + key + "}}", String(value));
        }

        const scriptFile = path.join(os.tmpdir(),
                `axiom-tool-${toolName}-${Date.now()}.sh`);
        fs.writeFileSync(scriptFile, cmd);
        log("DEBUG", "Executing script", { toolName, scriptFile });

        const startTime = Date.now();
        let result;
        try {
            result = execSync(`bash "${scriptFile}"`, {
                encoding: "utf-8",
                timeout: 30000,
                env: { ...process.env }
            });
        } finally {
            try { fs.unlinkSync(scriptFile); } catch (_) {}
        }
        const durationMs = Date.now() - startTime;

        log("INFO", "Script tool completed", { toolName, durationMs, outputLength: (result || "").length });
        return { content: [{ type: "text", text: result || "Command completed successfully" }] };
    } catch (error) {
        const msg = error.stderr || error.stdout || error.message || "Command failed";
        log("ERROR", "Script tool failed", { toolName, exitCode: error.status, error: msg.substring(0, 500) });
        return { content: [{ type: "text", text: msg }], isError: true };
    }
});

async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
}
main().catch(console.error);
