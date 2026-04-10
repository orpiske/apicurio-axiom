package io.apicurio.axiom.manager;

import io.apicurio.axiom.core.entities.ActionTypeEntity;
import io.apicurio.axiom.core.entities.ActorEntity;
import io.apicurio.axiom.core.entities.EventEntity;
import io.apicurio.axiom.core.entities.PolicyEntity;
import io.apicurio.axiom.core.entities.ProjectEntity;
import io.apicurio.axiom.core.entities.TaskEntity;

import java.util.List;

/**
 * Builds the system prompt and user prompt for the AI Manager.
 * Assembles event data, project context, policies, action types, and actors
 * into a comprehensive prompt that guides the Manager's decision-making.
 */
public final class ManagerPromptBuilder {

    private ManagerPromptBuilder() {
    }

    /**
     * Builds the system prompt that defines the Manager's role and available tools.
     *
     * @param policies the configured policies
     * @param actionTypes the registered action types
     * @param actors the configured actors
     * @return the system prompt
     */
    public static String buildSystemPrompt(List<PolicyEntity> policies,
                                            List<ActionTypeEntity> actionTypes,
                                            List<ActorEntity> actors) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are the Axiom Manager — an AI agent responsible for triaging events \
                from issue trackers (GitHub, Jira) and deciding what actions to take.

                When you receive an event, you must analyze it and return a JSON response \
                containing one or more decisions. Each decision must include:
                - "decision": one of "create_task", "ignore", "system_action", "escalate"
                - "actionType": the action type name (for create_task/system_action)
                - "actorHint": optional preferred actor name
                - "inputContext": context/instructions for the actor
                - "confidence": your confidence in this decision (0.0 to 1.0)
                - "reasoning": brief explanation of why you made this decision

                ## Guidelines
                - Multiple decisions are allowed for a single event (e.g., both auto-tag and analyze)
                - If you are uncertain, set a low confidence score
                - If you don't know what to do, use "escalate"
                - For trivial events (bot comments, automated labels), use "ignore"
                - System actions are: "close-project", "reopen-project"

                """);

        // Policies
        sb.append("## Policies\n\n");
        sb.append("These are your decision guidelines. Evaluate each policy against the event:\n\n");
        if (policies.isEmpty()) {
            sb.append("No policies configured.\n\n");
        } else {
            for (PolicyEntity policy : policies) {
                sb.append("### ").append(policy.name).append("\n");
                sb.append(policy.guideline).append("\n");
                if (policy.actionType != null) {
                    sb.append("Action type: ").append(policy.actionType).append("\n");
                }
                if (policy.actorHint != null) {
                    sb.append("Preferred actor: ").append(policy.actorHint).append("\n");
                }
                sb.append("\n");
            }
        }

        // Action types
        sb.append("## Available Action Types\n\n");
        for (ActionTypeEntity at : actionTypes) {
            sb.append("- **").append(at.name).append("** (").append(at.executionMode).append(")");
            if (at.description != null) {
                sb.append(": ").append(at.description);
            }
            sb.append("\n");
        }
        sb.append("\n");

        // Actors
        sb.append("## Available Actors\n\n");
        if (actors.isEmpty()) {
            sb.append("No actors configured.\n\n");
        } else {
            for (ActorEntity actor : actors) {
                sb.append("- **").append(actor.name).append("** (").append(actor.type).append(")");
                if (actor.description != null) {
                    sb.append(": ").append(actor.description);
                }
                if (actor.capabilities != null) {
                    sb.append(" [capabilities: ").append(actor.capabilities).append("]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Builds the user prompt containing the event to evaluate.
     *
     * @param event the event to evaluate
     * @param project the existing project for this issue (may be null)
     * @param recentTasks recent tasks for the project (may be empty)
     * @return the user prompt
     */
    public static String buildUserPrompt(EventEntity event, ProjectEntity project,
                                          List<TaskEntity> recentTasks) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Event to Evaluate\n\n");
        sb.append("- **Source:** ").append(event.source).append("\n");
        sb.append("- **Event type:** ").append(event.eventType).append("\n");
        if (event.issueRef != null) {
            sb.append("- **Issue:** ").append(event.issueRef).append("\n");
        }
        if (event.repository != null) {
            sb.append("- **Repository:** ").append(event.repository).append("\n");
        }
        sb.append("\n");

        sb.append("### Event Payload\n\n");
        sb.append("```json\n");
        sb.append(event.payload != null ? event.payload : "{}");
        sb.append("\n```\n\n");

        // Project context
        if (project != null) {
            sb.append("## Existing Project\n\n");
            sb.append("- **ID:** ").append(project.id).append("\n");
            sb.append("- **Name:** ").append(project.name).append("\n");
            sb.append("- **Status:** ").append(project.status).append("\n");
            sb.append("- **Type:** ").append(project.type).append("\n");
            sb.append("\n");

            if (!recentTasks.isEmpty()) {
                sb.append("### Recent Tasks\n\n");
                for (TaskEntity task : recentTasks) {
                    sb.append("- ").append(task.actionType)
                            .append(" (").append(task.status).append(")");
                    if (task.output != null && !task.output.isEmpty()) {
                        String preview = task.output.length() > 200
                                ? task.output.substring(0, 200) + "..."
                                : task.output;
                        sb.append(": ").append(preview);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("No existing project for this issue.\n\n");
        }

        sb.append("""
                Analyze this event and return ONLY a JSON object (no other text, no \
                markdown, no explanation) with a "decisions" array. Each element must \
                have: decision, actionType, actorHint, inputContext, confidence, reasoning. \
                Your entire response must be valid JSON and nothing else.
                """);

        return sb.toString();
    }

    /**
     * Returns the JSON schema that enforces the Manager's response format.
     * Used with Claude Code's {@code --json-schema} flag.
     */
    public static String getResponseJsonSchema() {
        // Must be a single-line compact JSON string for the CLI argument
        return "{\"type\":\"object\",\"required\":[\"decisions\"],\"properties\":{\"decisions\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"required\":[\"decision\",\"confidence\",\"reasoning\"],\"properties\":{\"decision\":{\"type\":\"string\",\"enum\":[\"create_task\",\"ignore\",\"system_action\",\"escalate\"]},\"actionType\":{\"type\":\"string\"},\"actorHint\":{\"type\":\"string\"},\"inputContext\":{\"type\":\"string\"},\"confidence\":{\"type\":\"number\",\"minimum\":0,\"maximum\":1},\"reasoning\":{\"type\":\"string\"}}}}}}";
    }
}
