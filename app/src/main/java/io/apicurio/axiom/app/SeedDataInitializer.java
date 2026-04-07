package io.apicurio.axiom.app;

import io.apicurio.axiom.core.entities.ActionTypeEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Seeds built-in action types on application startup if they don't already exist.
 */
@ApplicationScoped
public class SeedDataInitializer {

    private static final Logger LOG = Logger.getLogger(SeedDataInitializer.class);

    /**
     * Called on application startup to seed built-in action types.
     *
     * @param event the Quarkus startup event
     */
    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (ActionTypeEntity.count() > 0) {
            LOG.info("Action types already exist, skipping seed data");
            return;
        }

        LOG.info("Seeding built-in action types");

        seedActionType("analyze",
                "Read and understand an issue, assess complexity, identify affected components",
                "actor", true, true);

        seedActionType("auto-tag",
                "Determine appropriate labels/tags for an issue",
                "actor", false, true);

        seedActionType("implement",
                "Write code to address the issue",
                "actor", true, true);

        seedActionType("propose",
                "Draft a proposal or design for addressing the issue (read-only, no code changes)",
                "actor", true, true);

        seedActionType("review",
                "Review a pull request or code change",
                "actor", true, true);

        seedActionType("respond",
                "Reply to a comment or review feedback",
                "actor", true, true);

        seedActionType("answer-question",
                "Answer a question asked by a user on the issue",
                "actor", false, true);

        seedActionType("close-project",
                "Mark the project as completed",
                "system", false, false);

        seedActionType("reopen-project",
                "Re-open a completed project",
                "system", false, false);

        LOG.infof("Seeded %d built-in action types", ActionTypeEntity.count());
    }

    private void seedActionType(String name, String description, String executionMode,
                                boolean userTriggerable, boolean emitsEvent) {
        ActionTypeEntity entity = new ActionTypeEntity();
        entity.name = name;
        entity.description = description;
        entity.executionMode = executionMode;
        entity.userTriggerable = userTriggerable;
        entity.emitsEvent = emitsEvent;
        entity.persist();
    }
}
