package io.apitomy.axiom.app.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates generated JSON configuration files against the expected schemas
 * for Tools, Action Types, and Report Definitions.
 */
@ApplicationScoped
public class AssistantItemValidator {

    private static final Logger LOG = Logger.getLogger(AssistantItemValidator.class);

    private static final Set<String> VALID_EXECUTION_MODES = Set.of("actor", "script");
    private static final Set<String> VALID_SCHEDULES = Set.of(
            "none", "daily", "weekly", "monthly");
    private static final Set<String> VALID_TIME_WINDOWS = Set.of(
            "since-last-run", "last-24h", "last-7d", "last-30d");

    @Inject
    ObjectMapper objectMapper;

    /**
     * Validates a JSON file in the given subdirectory type.
     *
     * @param file the path to the JSON file
     * @param itemType the item type: "tools", "action-types", or "report-definitions"
     * @return a list of validation errors (empty if valid)
     */
    public List<String> validate(Path file, String itemType) {
        List<String> errors = new ArrayList<>();

        if (!Files.exists(file)) {
            errors.add("File does not exist: " + file.getFileName());
            return errors;
        }

        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            errors.add("Cannot read file: " + e.getMessage());
            return errors;
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(content);
        } catch (Exception e) {
            errors.add("Invalid JSON: " + e.getMessage());
            return errors;
        }

        if (!node.isObject()) {
            errors.add("Root element must be a JSON object");
            return errors;
        }

        switch (itemType) {
            case "tools" -> validateTool(node, errors);
            case "action-types" -> validateActionType(node, errors);
            case "report-definitions" -> validateReportDefinition(node, errors);
            default -> errors.add("Unknown item type: " + itemType);
        }

        return errors;
    }

    /**
     * Determines the item type from a file path based on its parent directory.
     *
     * @param file the file path relative to the working directory
     * @param workingDirectory the session's working directory
     * @return the item type, or null if the file is not in a known subdirectory
     */
    public String detectItemType(Path file, Path workingDirectory) {
        Path relative = workingDirectory.relativize(file);
        String first = relative.getName(0).toString();
        return switch (first) {
            case "tools" -> "tools";
            case "action-types" -> "action-types";
            case "report-definitions" -> "report-definitions";
            default -> null;
        };
    }

    private void validateTool(JsonNode node, List<String> errors) {
        requireString(node, "name", errors);
        requireString(node, "description", errors);
        requireString(node, "scriptTemplate", errors);

        JsonNode params = node.path("parameters");
        if (!params.isMissingNode() && !params.isNull()) {
            if (!params.isArray()) {
                errors.add("'parameters' must be a JSON array");
            } else {
                for (int i = 0; i < params.size(); i++) {
                    JsonNode param = params.get(i);
                    if (!param.has("name") || param.path("name").asText("").isBlank()) {
                        errors.add("Parameter at index " + i + " is missing 'name'");
                    }
                }
            }
        }

        JsonNode labels = node.path("labels");
        if (!labels.isMissingNode() && !labels.isNull() && !labels.isArray()) {
            errors.add("'labels' must be a JSON array of strings");
        }
    }

    private void validateActionType(JsonNode node, List<String> errors) {
        requireString(node, "name", errors);
        requireString(node, "description", errors);

        String executionMode = node.path("executionMode").asText("");
        if (executionMode.isBlank()) {
            errors.add("Missing required field: 'executionMode'");
        } else if (!VALID_EXECUTION_MODES.contains(executionMode)) {
            errors.add("Invalid executionMode '" + executionMode
                    + "'. Must be one of: " + VALID_EXECUTION_MODES);
        }

        if ("actor".equals(executionMode)) {
            if (!node.has("promptTemplate")
                    || node.path("promptTemplate").asText("").isBlank()) {
                errors.add("Actor-mode action types require a non-empty 'promptTemplate'");
            }
        } else if ("script".equals(executionMode)) {
            if (!node.has("scriptTemplate")
                    || node.path("scriptTemplate").asText("").isBlank()) {
                errors.add("Script-mode action types require a non-empty 'scriptTemplate'");
            }
        }
    }

    private void validateReportDefinition(JsonNode node, List<String> errors) {
        requireString(node, "name", errors);
        requireString(node, "description", errors);

        String schedule = node.path("schedule").asText("");
        if (schedule.isBlank()) {
            errors.add("Missing required field: 'schedule'");
        } else if (!VALID_SCHEDULES.contains(schedule) && !schedule.contains(" ")) {
            // Allow cron expressions (contain spaces) alongside preset values
            errors.add("Invalid schedule '" + schedule
                    + "'. Must be one of " + VALID_SCHEDULES + " or a cron expression");
        }

        String timeWindow = node.path("timeWindow").asText("");
        if (!timeWindow.isBlank() && !VALID_TIME_WINDOWS.contains(timeWindow)) {
            errors.add("Invalid timeWindow '" + timeWindow
                    + "'. Must be one of: " + VALID_TIME_WINDOWS);
        }

        if (!node.has("promptTemplate")
                || node.path("promptTemplate").asText("").isBlank()) {
            errors.add("Missing required field: 'promptTemplate'");
        }
    }

    private void requireString(JsonNode node, String field, List<String> errors) {
        if (!node.has(field) || node.path(field).asText("").isBlank()) {
            errors.add("Missing required field: '" + field + "'");
        }
    }
}
