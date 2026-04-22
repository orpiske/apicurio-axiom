package io.apicurio.axiom.app;

import io.apicurio.axiom.core.entities.ActionTypeEntity;
import io.apicurio.axiom.core.entities.ActorEntity;
import io.apicurio.axiom.core.entities.PolicyEntity;
import io.apicurio.axiom.core.entities.RepositoryEntity;
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
                "actor", true, true, READ_ONLY_TOOLS);

        seedActionType("auto-tag",
                "Determine appropriate labels/tags for an issue",
                "actor", false, true, READ_PLUS_GH_TOOLS);

        seedActionType("implement",
                "Write code to address the issue",
                "actor", true, true, WRITE_TOOLS);

        seedActionType("propose",
                "Draft a proposal or design for addressing the issue (read-only, no code changes)",
                "actor", true, true, READ_ONLY_TOOLS);

        seedActionType("review",
                "Review a pull request or code change",
                "actor", true, true, READ_ONLY_TOOLS);

        seedActionType("respond",
                "Reply to a comment or review feedback",
                "actor", true, true, WRITE_TOOLS);

        seedActionType("answer-question",
                "Answer a question asked by a user on the issue",
                "actor", false, true, READ_PLUS_GH_TOOLS);

        seedActionType("close-project",
                "Mark the project as completed",
                "system", false, false, null);

        seedActionType("reopen-project",
                "Re-open a completed project",
                "system", false, false, null);

        LOG.infof("Seeded %d built-in action types", ActionTypeEntity.count());

        // Seed actor, policy, and test repository
        seedActors();
        seedPolicy();
        seedRepository();
    }

    private void seedActors() {
        if (ActorEntity.count() > 0) {
            LOG.info("Actors already exist, skipping actor seed data");
            return;
        }

        ActorEntity actor = new ActorEntity();
        actor.name = "Blinky";
        actor.description = "AI agent powered by Claude Code CLI";
        actor.type = "ai-agent";
        actor.capabilities = "analyze,auto-tag,implement,propose,review,respond,answer-question";
        actor.persist();
        LOG.infof("Seeded actor: %s (%s)", actor.name, actor.type);

        actor = new ActorEntity();
        actor.name = "Clyde";
        actor.description = "AI agent powered by Claude Code CLI";
        actor.type = "ai-agent";
        actor.capabilities = "analyze,auto-tag,implement,propose,review,respond,answer-question";
        actor.persist();
        LOG.infof("Seeded actor: %s (%s)", actor.name, actor.type);
    }

    private void seedPolicy() {
        if (PolicyEntity.count() > 0) {
            LOG.info("Policies already exist, skipping policy seed data");
            return;
        }

        PolicyEntity policy = new PolicyEntity();
        policy.name = "Analyze new issues";
        policy.guideline = "If the event represents a new issue being created (issue-created), " +
                "perform the \"analyze\" action. The actor should read the issue title and body, " +
                "understand the problem or request, assess its complexity, and produce a brief " +
                "summary with recommendations for next steps.";
        policy.actionType = "analyze";
        policy.persist();

        LOG.infof("Seeded policy: %s", policy.name);
    }

    private void seedRepository() {
        if (RepositoryEntity.count() > 0) {
            LOG.info("Repositories already exist, skipping repository seed data");
            return;
        }

        RepositoryEntity repo = new RepositoryEntity();
        repo.name = "cb-test-project";
        repo.owner = "EricWittmann";
        repo.source = "github";
        repo.url = "https://github.com/EricWittmann/cb-test-project";
        repo.pollInterval = 30;
        repo.pollingEnabled = true;
        repo.persist();

        LOG.infof("Seeded test repository: %s/%s (polling every %ds)",
                repo.owner, repo.name, repo.pollInterval);
    }

    private static final String READ_ONLY_TOOLS = String.join(",",
            "Read", "Glob", "Grep",
            "Bash(ls *)", "Bash(cat *)", "Bash(head *)", "Bash(tail *)",
            "Bash(find *)", "Bash(wc *)", "Bash(file *)",
            "Bash(git log *)", "Bash(git diff *)", "Bash(git show *)",
            "Bash(git status *)", "Bash(git branch *)"
    );

    private static final String READ_PLUS_GH_TOOLS = String.join(",",
            READ_ONLY_TOOLS,
            "Write",
            "Bash(cat *)", "Bash(echo *)",
            "Bash(gh issue *)", "Bash(gh api *)"
    );

    private static final String WRITE_TOOLS = String.join(",",
            READ_ONLY_TOOLS,
            "Edit", "Write",
            "Bash(git add *)", "Bash(git commit *)", "Bash(git checkout *)",
            "Bash(git switch *)", "Bash(git push *)", "Bash(git merge *)",
            "Bash(gh issue *)", "Bash(gh pr *)", "Bash(gh api *)",
            "Bash(mkdir *)", "Bash(cp *)", "Bash(mv *)"
    );

    private void seedActionType(String name, String description, String executionMode,
                                boolean userTriggerable, boolean emitsEvent, String allowedTools) {
        ActionTypeEntity entity = new ActionTypeEntity();
        entity.name = name;
        entity.description = description;
        entity.executionMode = executionMode;
        entity.userTriggerable = userTriggerable;
        entity.emitsEvent = emitsEvent;
        entity.allowedTools = allowedTools;
        entity.persist();
    }
}
