package io.apicurio.axiom.core.lifecycle;

/**
 * Thrown when an invalid project status transition is attempted.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final ProjectStatus from;
    private final ProjectStatus to;

    /**
     * Creates a new exception for an invalid transition.
     *
     * @param from the current status
     * @param to the attempted target status
     */
    public InvalidStatusTransitionException(ProjectStatus from, ProjectStatus to) {
        super("Invalid project status transition: " + from + " → " + to);
        this.from = from;
        this.to = to;
    }

    /**
     * @return the current status
     */
    public ProjectStatus getFrom() {
        return from;
    }

    /**
     * @return the attempted target status
     */
    public ProjectStatus getTo() {
        return to;
    }
}
