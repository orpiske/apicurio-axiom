package io.apitomy.axiom.engine.spi;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all available AI engine implementations. Unlike the
 * {@link AiEngineProducer} (which resolves a single default engine),
 * this registry provides access to <em>any</em> engine by type, enabling
 * per-action-type engine selection.
 *
 * <p>Usage:</p>
 * <pre>
 * // Get the engine for a specific action type (falls back to default)
 * AiEngine engine = registry.getEngine(actionType.engine);
 *
 * // Get the actor type for a specific engine
 * String actorType = registry.getActorType("opencode");
 * </pre>
 */
@ApplicationScoped
public class AiEngineRegistry {

    private static final Logger LOG = Logger.getLogger(AiEngineRegistry.class);

    @ConfigProperty(name = "axiom.ai-engine", defaultValue = "claude-code")
    String defaultEngineType;

    @Inject
    Instance<AiEngineProvider> providers;

    private final Map<String, AiEngineProvider> providerMap = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        for (AiEngineProvider provider : providers) {
            providerMap.put(provider.getType(), provider);
            LOG.infof("Registered AI engine: %s (%s)",
                    provider.getType(), provider.getEngine().getClass().getSimpleName());
        }
        LOG.infof("AI engine registry: %d engine(s) available, default: %s",
                providerMap.size(), defaultEngineType);
    }

    /**
     * Returns the engine for the given type, falling back to the default engine
     * if the type is null, blank, or not found.
     *
     * @param engineType the engine type (e.g. "opencode"), or null for default
     * @return the resolved AI engine
     */
    public AiEngine getEngine(String engineType) {
        if (engineType != null && !engineType.isBlank()) {
            AiEngineProvider provider = providerMap.get(engineType);
            if (provider != null) {
                return provider.getEngine();
            }
            LOG.warnf("Unknown engine type '%s', falling back to default '%s'",
                    engineType, defaultEngineType);
        }
        return getDefaultEngine();
    }

    /**
     * Returns the MCP manager for the given engine type, falling back to the
     * default engine's MCP manager.
     */
    public AiEngineMcpManager getMcpManager(String engineType) {
        if (engineType != null && !engineType.isBlank()) {
            AiEngineProvider provider = providerMap.get(engineType);
            if (provider != null && provider.getMcpManager() != null) {
                return provider.getMcpManager();
            }
        }
        AiEngineProvider defaultProvider = providerMap.get(defaultEngineType);
        if (defaultProvider != null && defaultProvider.getMcpManager() != null) {
            return defaultProvider.getMcpManager();
        }
        return (taskId, environment, allowedTools) -> null;
    }

    /**
     * Returns the actor type identifier for the given engine type.
     * Used by TaskExecutionService to map "ai-agent" entities to the
     * correct Actor CDI bean.
     */
    public String getActorType(String engineType) {
        return getEngine(engineType).getActorType();
    }

    /**
     * Returns the default engine.
     */
    public AiEngine getDefaultEngine() {
        AiEngineProvider provider = providerMap.get(defaultEngineType);
        if (provider == null) {
            throw new IllegalStateException("Default AI engine '" + defaultEngineType + "' not found");
        }
        return provider.getEngine();
    }

    /**
     * Returns the default engine type identifier.
     */
    public String getDefaultEngineType() {
        return defaultEngineType;
    }

    /**
     * Returns the list of available engine type identifiers.
     */
    public List<String> getAvailableEngineTypes() {
        return List.copyOf(providerMap.keySet());
    }
}
