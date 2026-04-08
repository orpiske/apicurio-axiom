package io.apicurio.axiom.manager;

/**
 * Represents a single decision made by the AI Manager after evaluating an event.
 * The Manager may return one or more decisions per event.
 */
public record ManagerDecision(
        /**
         * The type of decision: "create_task", "ignore", "system_action", "escalate"
         */
        String decision,

        /**
         * The action type for the task (for create_task decisions), or the system
         * action name (for system_action decisions). Null for ignore/escalate.
         */
        String actionType,

        /**
         * Optional hint for which actor should be assigned the task.
         */
        String actorHint,

        /**
         * Context or instructions to pass to the actor as task input.
         */
        String inputContext,

        /**
         * The Manager's self-assessed confidence in this decision (0.0 to 1.0).
         */
        double confidence,

        /**
         * The Manager's reasoning for this decision.
         */
        String reasoning
) {

    /**
     * @return true if this is a create_task decision
     */
    public boolean isCreateTask() {
        return "create_task".equals(decision);
    }

    /**
     * @return true if this is an ignore decision
     */
    public boolean isIgnore() {
        return "ignore".equals(decision);
    }

    /**
     * @return true if this is a system action decision
     */
    public boolean isSystemAction() {
        return "system_action".equals(decision);
    }

    /**
     * @return true if this is an escalation to the user
     */
    public boolean isEscalate() {
        return "escalate".equals(decision);
    }
}
