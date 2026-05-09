package io.apicurio.axiom.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.axiom.core.entities.EventSourceEntity;
import io.apicurio.axiom.core.entities.ProjectEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages git clone workspaces for projects. Each project gets an independent
 * clone of its associated repository in a workspace directory.
 */
@ApplicationScoped
public class WorkspaceService {

    private static final Logger LOG = Logger.getLogger(WorkspaceService.class);

    @ConfigProperty(name = "axiom.workspace.root", defaultValue = "${user.home}/.axiom/workspaces")
    String workspaceRoot;

    @ConfigProperty(name = "axiom.workspace.clone-timeout-seconds", defaultValue = "120")
    int cloneTimeoutSeconds;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Returns the workspace path for a project. Creates the directory if it
     * doesn't exist.
     *
     * @param project the project
     * @return the workspace directory path
     */
    public Path getWorkspacePath(ProjectEntity project) {
        return Path.of(workspaceRoot, "project-" + project.id);
    }

    /**
     * Ensures a workspace exists for the given project. If the workspace
     * directory doesn't exist or is empty, clones the repository.
     *
     * @param project the project to create a workspace for
     * @return the workspace directory path
     * @throws WorkspaceException if the clone fails
     */
    public Path ensureWorkspace(ProjectEntity project) {
        Path workspace = getWorkspacePath(project);

        if (Files.exists(workspace.resolve(".git"))) {
            LOG.debugf("Workspace already exists for project %d at %s", project.id, workspace);
            return workspace;
        }

        LOG.infof("Creating workspace for project %d at %s", project.id, workspace);

        // Look up the event source configuration for clone URL and auth
        EventSourceEntity source = findEventSource(project);
        String cloneUrl = buildCloneUrl(project, source);

        try {
            Files.createDirectories(workspace.getParent());
            cloneRepository(cloneUrl, workspace, source);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to clone repository for project %d", project.id);
            throw new WorkspaceException("Failed to clone repository: " + e.getMessage(), e);
        }

        return workspace;
    }

    /**
     * Deletes the workspace directory for a project.
     *
     * @param project the project whose workspace should be deleted
     */
    public void deleteWorkspace(ProjectEntity project) {
        Path workspace = getWorkspacePath(project);
        if (Files.exists(workspace)) {
            LOG.infof("Deleting workspace for project %d at %s", project.id, workspace);
            try {
                deleteRecursively(workspace);
            } catch (IOException e) {
                LOG.warnf(e, "Failed to delete workspace for project %d", project.id);
            }
        }
    }

    /**
     * Computes the total disk usage of a project's workspace directory in bytes.
     * Walks the directory tree and sums the sizes of all files.
     *
     * @param project the project whose workspace size to compute
     * @return the total size in bytes, or 0 if the workspace doesn't exist
     */
    public long computeDiskUsage(ProjectEntity project) {
        Path workspace = getWorkspacePath(project);
        if (!Files.exists(workspace)) {
            return 0;
        }
        try {
            // Use du -s --block-size=1 for disk-allocated size in bytes
            ProcessBuilder pb = new ProcessBuilder("du", "-s", "--block-size=1",
                    workspace.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0 && !output.isEmpty()) {
                // du -s --block-size=1 outputs: <bytes>\t<path>
                return Long.parseLong(output.split("\\s+")[0]);
            }
        } catch (Exception e) {
            LOG.debugf("du command failed for project %d, falling back to file walk", project.id);
        }

        // Fallback: walk directory and sum file sizes
        try (var stream = Files.walk(workspace)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); }
                        catch (IOException e) { return 0; }
                    })
                    .sum();
        } catch (IOException e) {
            LOG.warnf(e, "Failed to compute disk usage for project %d", project.id);
            return 0;
        }
    }

    /**
     * Finds the EventSourceEntity matching a project's repository field.
     * The project's repository field is in "owner/repo" format. This method
     * iterates GitHub event sources and parses their configuration JSON
     * to match owner and name.
     *
     * @param project the project whose event source to find
     * @return the matching event source, or null if not found
     */
    private EventSourceEntity findEventSource(ProjectEntity project) {
        String[] parts = project.repository.split("/", 2);
        if (parts.length == 2) {
            String targetOwner = parts[0];
            String targetName = parts[1];

            List<EventSourceEntity> sources = EventSourceEntity.list("sourceType", "github");
            for (EventSourceEntity source : sources) {
                try {
                    JsonNode config = objectMapper.readTree(source.configuration);
                    String owner = config.path("owner").asText(null);
                    String name = config.path("name").asText(null);
                    if (targetOwner.equals(owner) && targetName.equals(name)) {
                        return source;
                    }
                } catch (Exception e) {
                    LOG.debugf("Failed to parse configuration for event source %d", source.id);
                }
            }
        }
        return null;
    }

    /**
     * Builds the clone URL for a project, extracting the URL from the event source's
     * configuration JSON if available.
     *
     * @param project the project
     * @param source the event source (may be null)
     * @return the clone URL
     */
    private String buildCloneUrl(ProjectEntity project, EventSourceEntity source) {
        if (source != null && source.configuration != null) {
            try {
                JsonNode config = objectMapper.readTree(source.configuration);
                String url = config.path("url").asText(null);
                if (url != null) {
                    // If the source has a PAT in its configuration, embed it in the URL
                    String pat = extractPat(source);
                    if (pat != null) {
                        // https://<token>@github.com/owner/repo.git
                        return url.replace("https://", "https://" + pat + "@");
                    }
                    return url;
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse configuration for event source %d", source.id);
            }
        }
        // Fall back to constructing a URL from the project's repository field
        return "https://github.com/" + project.repository + ".git";
    }

    /**
     * Extracts a personal access token from the event source's configuration JSON.
     *
     * @param source the event source
     * @return the PAT, or null if not found
     */
    private String extractPat(EventSourceEntity source) {
        // PAT can be stored in the event source's configuration JSON
        // For now, return null — SSH/PAT integration will be refined later
        return null;
    }

    private void cloneRepository(String cloneUrl, Path targetDir, EventSourceEntity source)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "git", "clone", "--depth", "1", cloneUrl, targetDir.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        // Configure SSH if needed
        if (source != null && source.configuration != null
                && source.configuration.contains("sshKeyPath")) {
            // TODO: Parse SSH key path from configuration and set GIT_SSH_COMMAND
            LOG.debugf("SSH authentication configured for event source %s", source.name);
        }

        Process process = pb.start();
        boolean completed = process.waitFor(cloneTimeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Git clone timed out after " + cloneTimeoutSeconds + "s");
        }
        if (process.exitValue() != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new IOException("Git clone failed (exit " + process.exitValue() + "): " + output);
        }

        LOG.infof("Cloned repository to %s", targetDir);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    /**
     * Exception thrown when workspace operations fail.
     */
    public static class WorkspaceException extends RuntimeException {

        /**
         * Creates a new workspace exception.
         *
         * @param message the error message
         * @param cause the underlying cause
         */
        public WorkspaceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
