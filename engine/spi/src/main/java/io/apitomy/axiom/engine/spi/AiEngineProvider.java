package io.apitomy.axiom.engine.spi;

/**
 * Registration interface for AI engine implementations. Each engine module
 * provides a CDI bean implementing this interface, which the {@link AiEngineProducer}
 * uses to discover and select the active engine.
 *
 * <p>This indirection avoids CDI bean type conflicts: engine implementations
 * are registered as {@code AiEngineProvider} beans rather than as {@code AiEngine}
 * beans directly, so the producer can iterate providers without causing
 * infinite recursion or ambiguous bean resolution.</p>
 */
public interface AiEngineProvider {

    /**
     * Returns the engine type identifier (e.g. "claude-code", "opencode").
     */
    String getType();

    /**
     * Returns the engine implementation.
     */
    AiEngine getEngine();

    /**
     * Returns the MCP manager for this engine, or null if not supported.
     */
    default AiEngineMcpManager getMcpManager() {
        return null;
    }
}
