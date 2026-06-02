package io.apitomy.axiom.actors.claudecode;

import io.apitomy.axiom.actors.spi.ActorContext;
import io.apitomy.axiom.engine.spi.AiEngine;
import io.apitomy.axiom.engine.spi.AiEngineCheckResult;
import io.apitomy.axiom.engine.spi.AiEngineConfig;
import io.apitomy.axiom.engine.spi.AiEngineMcpManager;
import io.apitomy.axiom.engine.spi.AiEngineProvider;
import io.apitomy.axiom.engine.spi.AiEngineResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Claude Code CLI implementation of the {@link AiEngine} SPI. Wraps the existing
 * {@link ClaudeCodeCommandBuilder} and {@link ClaudeCodeSubprocess} to provide
 * engine-agnostic prompt execution with structured output support.
 *
 * <p>Also serves as the {@link AiEngineProvider} for CDI discovery — the
 * {@link io.apitomy.axiom.engine.spi.AiEngineProducer} finds this bean via
 * the provider interface to avoid CDI bean type recursion.</p>
 */
@ApplicationScoped
@Typed({ClaudeCodeEngine.class, AiEngineProvider.class})
public class ClaudeCodeEngine implements AiEngine, AiEngineProvider {

    @ConfigProperty(name = "axiom.claude-code.executable", defaultValue = "claude")
    String executable;

    @Inject
    ClaudeCodeMcpManager mcpManager;

    @Override
    public String getType() {
        return "claude-code";
    }

    @Override
    public CompletableFuture<AiEngineResult> prompt(AiEngineConfig config, String prompt) {
        return executeInternal(config, prompt, null);
    }

    @Override
    public CompletableFuture<AiEngineResult> promptWithSchema(AiEngineConfig config,
                                                               String prompt,
                                                               String jsonSchema) {
        return executeInternal(config, prompt, jsonSchema);
    }

    @Override
    public List<AiEngineCheckResult> healthCheck() {
        List<AiEngineCheckResult> results = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(executable, "-p", "Reply with exactly: AXIOM_OK",
                    "--bare", "--output-format", "text", "--max-turns", "1");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                results.add(new AiEngineCheckResult(
                        "Claude Code CLI",
                        "error",
                        "Claude Code CLI check timed out after 30 seconds. "
                                + "Ensure that the 'claude' command is on your PATH and that "
                                + "your Anthropic API key is configured correctly."
                ));
                return results;
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            if (exitCode == 0 && output.contains("AXIOM_OK")) {
                results.add(new AiEngineCheckResult(
                        "Claude Code CLI",
                        "ok",
                        "Claude Code CLI is available and working."
                ));
            } else {
                results.add(new AiEngineCheckResult(
                        "Claude Code CLI",
                        "error",
                        "Claude Code CLI returned exit code " + exitCode + ". "
                                + "Ensure that the 'claude' command is installed, on your PATH, "
                                + "and that ANTHROPIC_API_KEY is set in your environment. "
                                + "Install Claude Code: npm install -g @anthropic-ai/claude-code"
                ));
            }
        } catch (Exception e) {
            results.add(new AiEngineCheckResult(
                    "Claude Code CLI",
                    "error",
                    "Claude Code CLI is not available: " + e.getMessage() + ". "
                            + "Install Claude Code: npm install -g @anthropic-ai/claude-code"
            ));
        }

        return results;
    }

    private CompletableFuture<AiEngineResult> executeInternal(AiEngineConfig config,
                                                               String prompt,
                                                               String jsonSchema) {
        // Build ActorContext from AiEngineConfig for the command builder
        ActorContext actorContext = ActorContext.builder()
                .systemPrompt(config.getSystemPrompt())
                .allowedTools(config.getAllowedTools())
                .disallowedTools(config.getDisallowedTools())
                .workingDirectory(config.getWorkingDirectory())
                .mcpConfigFile(config.getMcpConfigFile())
                .build();

        // Build execution log
        ExecutionLogBuilder logBuilder = new ExecutionLogBuilder();
        logBuilder.header(0, "ai-engine", Instant.now());
        if (config.getSystemPrompt() != null) {
            logBuilder.systemPrompt(config.getSystemPrompt());
        }
        logBuilder.prompt(prompt);
        logBuilder.allowedTools(config.getAllowedTools());

        // Build command
        ClaudeCodeCommandBuilder cmdBuilder = ClaudeCodeCommandBuilder
                .fromContext(prompt, actorContext)
                .executable(executable)
                .streamJson(true)
                .maxTurns(config.getMaxSteps());

        if (config.getModel() != null) {
            cmdBuilder.model(config.getModel());
        }
        if (config.getMaxBudgetUsd() != null) {
            cmdBuilder.maxBudgetUsd(config.getMaxBudgetUsd());
        }
        if (config.getSessionId() != null) {
            cmdBuilder.sessionId(config.getSessionId());
        }

        List<String> command = cmdBuilder.build();

        // Add json-schema flag if structured output is requested
        if (jsonSchema != null) {
            command.add("--json-schema");
            command.add(jsonSchema);
        }

        // Determine working directory as File
        java.io.File workDir = config.getWorkingDirectory() != null
                ? config.getWorkingDirectory().toFile()
                : null;

        // Execute subprocess
        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                command, workDir,
                config.getEnvironment() != null ? config.getEnvironment() : Map.of(),
                Duration.ofSeconds(config.getTimeoutSeconds()),
                null, logBuilder
        );

        return subprocess.execute().thenApply(ClaudeCodeEngine::toEngineResult);
    }

    /**
     * Maps a Claude Code result to the engine-agnostic result type.
     */
    private static AiEngineResult toEngineResult(ClaudeCodeResult result) {
        return new AiEngineResult(
                result.result(),
                result.sessionId(),
                result.totalCostUsd(),
                result.inputTokens(),
                result.outputTokens(),
                result.isSuccess(),
                result.executionLog()
        );
    }

    // --- AiEngineProvider ---

    @Override
    public AiEngine getEngine() {
        return this;
    }

    @Override
    public AiEngineMcpManager getMcpManager() {
        return mcpManager;
    }
}
