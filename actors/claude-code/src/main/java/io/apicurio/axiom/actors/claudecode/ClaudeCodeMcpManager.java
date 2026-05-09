package io.apicurio.axiom.actors.claudecode;

import io.apicurio.axiom.engine.spi.AiEngineMcpManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Claude Code implementation of {@link AiEngineMcpManager}. Delegates to the
 * existing {@link io.apicurio.axiom.app.McpConfigGenerator} to produce
 * {@code --mcp-config} JSON files for Claude Code subprocesses.
 *
 * <p>Note: This is a thin adapter. The actual MCP config generation logic
 * remains in {@code McpConfigGenerator} (in the app module) and is injected
 * here. This class exists to satisfy the engine SPI contract so that
 * consumers can use {@link AiEngineMcpManager} without coupling to
 * Claude-specific code.</p>
 */
@ApplicationScoped
@Typed(ClaudeCodeMcpManager.class)
public class ClaudeCodeMcpManager implements AiEngineMcpManager {

    /**
     * Functional interface for the MCP config generation, allowing the app module's
     * McpConfigGenerator to be injected without creating a circular dependency.
     */
    @FunctionalInterface
    public interface McpConfigDelegate {
        Path generateMcpConfig(Long taskId, Map<String, String> environment,
                               List<String> allowedTools);
    }

    private volatile McpConfigDelegate delegate;

    /**
     * Sets the delegate that performs the actual MCP config generation.
     * Called by the app module during initialization.
     *
     * @param delegate the MCP config generation delegate
     */
    public void setDelegate(McpConfigDelegate delegate) {
        this.delegate = delegate;
        LOG.infof("MCP config delegate set on ClaudeCodeMcpManager instance@%d",
                System.identityHashCode(this));
    }

    private static final org.jboss.logging.Logger LOG =
            org.jboss.logging.Logger.getLogger(ClaudeCodeMcpManager.class);

    @Override
    public Path configureMcpServers(Long taskId, Map<String, String> environment,
                                     List<String> allowedTools) {
        if (delegate != null) {
            Path result = delegate.generateMcpConfig(taskId, environment, allowedTools);
            LOG.debugf("MCP config for task %d: %s", taskId, result);
            return result;
        }
        LOG.warnf("MCP config delegate not set on ClaudeCodeMcpManager instance@%d — cannot generate MCP config for task %d",
                System.identityHashCode(this), taskId);
        return null;
    }
}
