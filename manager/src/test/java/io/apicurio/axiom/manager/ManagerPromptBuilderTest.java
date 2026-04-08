package io.apicurio.axiom.manager;

import io.apicurio.axiom.core.entities.ActionTypeEntity;
import io.apicurio.axiom.core.entities.ActorEntity;
import io.apicurio.axiom.core.entities.EventEntity;
import io.apicurio.axiom.core.entities.PolicyEntity;
import io.apicurio.axiom.core.entities.ProjectEntity;
import io.apicurio.axiom.core.entities.TaskEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ManagerPromptBuilder.
 */
class ManagerPromptBuilderTest {

    @Test
    void testSystemPromptContainsRole() {
        String prompt = ManagerPromptBuilder.buildSystemPrompt(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        assertTrue(prompt.contains("Axiom Manager"));
        assertTrue(prompt.contains("create_task"));
        assertTrue(prompt.contains("ignore"));
        assertTrue(prompt.contains("system_action"));
        assertTrue(prompt.contains("escalate"));
    }

    @Test
    void testSystemPromptContainsPolicies() {
        PolicyEntity policy = new PolicyEntity();
        policy.name = "Auto-tag issues";
        policy.guideline = "Tag new issues with appropriate labels";
        policy.actionType = "auto-tag";

        String prompt = ManagerPromptBuilder.buildSystemPrompt(
                List.of(policy), Collections.emptyList(), Collections.emptyList());

        assertTrue(prompt.contains("Auto-tag issues"));
        assertTrue(prompt.contains("Tag new issues"));
        assertTrue(prompt.contains("auto-tag"));
    }

    @Test
    void testSystemPromptContainsActionTypes() {
        ActionTypeEntity at = new ActionTypeEntity();
        at.name = "analyze";
        at.executionMode = "actor";
        at.description = "Analyze an issue";

        String prompt = ManagerPromptBuilder.buildSystemPrompt(
                Collections.emptyList(), List.of(at), Collections.emptyList());

        assertTrue(prompt.contains("analyze"));
        assertTrue(prompt.contains("actor"));
        assertTrue(prompt.contains("Analyze an issue"));
    }

    @Test
    void testSystemPromptContainsActors() {
        ActorEntity actor = new ActorEntity();
        actor.name = "Claude Agent";
        actor.type = "ai-agent";
        actor.description = "AI agent powered by Claude";
        actor.capabilities = "analyze,implement";

        String prompt = ManagerPromptBuilder.buildSystemPrompt(
                Collections.emptyList(), Collections.emptyList(), List.of(actor));

        assertTrue(prompt.contains("Claude Agent"));
        assertTrue(prompt.contains("ai-agent"));
        assertTrue(prompt.contains("analyze,implement"));
    }

    @Test
    void testUserPromptContainsEventDetails() {
        EventEntity event = new EventEntity();
        event.source = "github";
        event.eventType = "issue-created";
        event.issueRef = "Apicurio/axiom#42";
        event.repository = "Apicurio/axiom";
        event.payload = "{\"action\":\"opened\",\"issue\":{\"title\":\"Test\"}}";

        String prompt = ManagerPromptBuilder.buildUserPrompt(
                event, null, Collections.emptyList());

        assertTrue(prompt.contains("github"));
        assertTrue(prompt.contains("issue-created"));
        assertTrue(prompt.contains("Apicurio/axiom#42"));
        assertTrue(prompt.contains("\"action\":\"opened\""));
        assertTrue(prompt.contains("No existing project"));
    }

    @Test
    void testUserPromptContainsProjectContext() {
        EventEntity event = new EventEntity();
        event.source = "github";
        event.eventType = "comment-added";
        event.issueRef = "Apicurio/axiom#10";
        event.payload = "{}";

        ProjectEntity project = new ProjectEntity();
        project.id = 1L;
        project.name = "Fix login bug";
        project.status = "Idle";
        project.type = "bug-fix";

        TaskEntity task = new TaskEntity();
        task.actionType = "analyze";
        task.status = "Completed";
        task.output = "The issue is in the auth module";

        String prompt = ManagerPromptBuilder.buildUserPrompt(
                event, project, List.of(task));

        assertTrue(prompt.contains("Fix login bug"));
        assertTrue(prompt.contains("Idle"));
        assertTrue(prompt.contains("bug-fix"));
        assertTrue(prompt.contains("analyze"));
        assertTrue(prompt.contains("Completed"));
        assertTrue(prompt.contains("auth module"));
    }

    @Test
    void testJsonSchemaIsValidJson() {
        String schema = ManagerPromptBuilder.getResponseJsonSchema();

        assertNotNull(schema);
        assertTrue(schema.contains("\"decisions\""));
        assertTrue(schema.contains("\"create_task\""));
        assertTrue(schema.contains("\"confidence\""));
        // Verify it's parseable JSON
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(schema));
    }
}
