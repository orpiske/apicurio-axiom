package io.apitomy.axiom.core.lifecycle;

/**
 * Defines the valid project lifecycle states.
 */
public enum ProjectStatus {

    Created,
    InProgress,
    Idle,
    Completed;

    /**
     * Parses a status string into a ProjectStatus enum value.
     *
     * @param value the status string
     * @return the matching ProjectStatus
     * @throws IllegalArgumentException if the value doesn't match any status
     */
    public static ProjectStatus fromValue(String value) {
        for (ProjectStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown project status: " + value);
    }
}
