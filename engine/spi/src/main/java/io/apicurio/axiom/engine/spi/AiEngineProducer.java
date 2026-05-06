package io.apicurio.axiom.engine.spi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * CDI producer that resolves the active {@link AiEngine} and {@link AiEngineMcpManager}
 * implementations based on the {@code axiom.ai-engine} configuration property.
 *
 * <p>Engine implementations use {@code @Typed(ConcreteClass.class)} to avoid being
 * discovered as {@code AiEngine} beans directly (which would conflict with the
 * produced bean). Instead, they register themselves as {@link AiEngineProvider}
 * beans, which this producer iterates to find the matching engine type.</p>
 */
@ApplicationScoped
public class AiEngineProducer {

    private static final Logger LOG = Logger.getLogger(AiEngineProducer.class);

    @ConfigProperty(name = "axiom.ai-engine", defaultValue = "claude-code")
    String aiEngineType;

    @Produces
    @ApplicationScoped
    public AiEngine produceAiEngine(Instance<AiEngineProvider> providers) {
        LOG.infof("Resolving AI engine: %s", aiEngineType);

        for (AiEngineProvider provider : providers) {
            if (provider.getType().equals(aiEngineType)) {
                AiEngine engine = provider.getEngine();
                LOG.infof("AI engine resolved: %s (%s)", aiEngineType,
                        engine.getClass().getSimpleName());
                return engine;
            }
        }

        throw new IllegalStateException(
                "No AI engine implementation found for type '" + aiEngineType + "'. "
                        + "Ensure that the corresponding engine module is on the classpath "
                        + "and provides a CDI bean implementing AiEngineProvider."
        );
    }

    @Produces
    @ApplicationScoped
    public AiEngineMcpManager produceAiEngineMcpManager(Instance<AiEngineProvider> providers) {
        for (AiEngineProvider provider : providers) {
            if (provider.getType().equals(aiEngineType)) {
                AiEngineMcpManager mcpManager = provider.getMcpManager();
                if (mcpManager != null) {
                    LOG.infof("AI engine MCP manager resolved: %s (%s)",
                            aiEngineType, mcpManager.getClass().getSimpleName());
                    return mcpManager;
                }
            }
        }

        LOG.warnf("No AiEngineMcpManager found for engine type '%s', MCP features may be unavailable",
                aiEngineType);
        return (taskId, environment, allowedTools) -> null;
    }
}
