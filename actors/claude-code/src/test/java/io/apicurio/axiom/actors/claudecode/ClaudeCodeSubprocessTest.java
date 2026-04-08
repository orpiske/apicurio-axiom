package io.apicurio.axiom.actors.claudecode;

import io.apicurio.axiom.actors.spi.ActorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that actually launch the Claude Code CLI.
 *
 * <p>These tests are <strong>disabled by default</strong>. To enable them, set the
 * environment variable {@code AXIOM_CLAUDE_TESTS=true} before running:</p>
 *
 * <pre>
 * AXIOM_CLAUDE_TESTS=true mvn test -pl actors/claude-code
 * </pre>
 *
 * <p>Requirements:</p>
 * <ul>
 *   <li>The {@code claude} CLI must be installed and on the PATH</li>
 *   <li>A valid {@code ANTHROPIC_API_KEY} must be set in the environment</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "AXIOM_CLAUDE_TESTS", matches = "true")
class ClaudeCodeSubprocessTest {

    @TempDir
    Path tempDir;

    @Test
    void testSimplePrompt() throws ExecutionException, InterruptedException, TimeoutException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .build();

        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("Reply with exactly: HELLO AXIOM", context)
                .streamJson(false)
                .maxTurns(1)
                .maxBudgetUsd(0.10)
                .build();

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(60), null);

        ClaudeCodeResult result = subprocess.execute()
                .get(90, TimeUnit.SECONDS);

        assertTrue(result.isSuccess(), "Expected success but got exit code " + result.exitCode());
        assertNotNull(result.result(), "Result text should not be null");
        assertTrue(result.result().contains("HELLO AXIOM"),
                "Expected result to contain 'HELLO AXIOM' but got: " + result.result());
    }

    @Test
    void testJsonOutputContainsCostAndTokens()
            throws ExecutionException, InterruptedException, TimeoutException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .build();

        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("Say 'ok'", context)
                .streamJson(false)
                .maxTurns(1)
                .maxBudgetUsd(0.10)
                .build();

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(60), null);

        ClaudeCodeResult result = subprocess.execute()
                .get(90, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertNotNull(result.sessionId(), "Session ID should be present");
        assertNotNull(result.totalCostUsd(), "Cost should be tracked");
        assertTrue(result.totalCostUsd() > 0, "Cost should be positive");
        assertNotNull(result.inputTokens(), "Input tokens should be tracked");
        assertTrue(result.inputTokens() > 0, "Input tokens should be positive");
        assertNotNull(result.outputTokens(), "Output tokens should be tracked");
        assertTrue(result.outputTokens() > 0, "Output tokens should be positive");
    }

    @Test
    void testStreamJsonOutput()
            throws ExecutionException, InterruptedException, TimeoutException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .build();

        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("Say 'streaming works'", context)
                .streamJson(true)
                .maxTurns(1)
                .maxBudgetUsd(0.10)
                .build();

        java.util.concurrent.atomic.AtomicInteger lineCount =
                new java.util.concurrent.atomic.AtomicInteger(0);

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(60),
                line -> lineCount.incrementAndGet());

        ClaudeCodeResult result = subprocess.execute()
                .get(90, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertTrue(lineCount.get() > 0, "Should have received streaming output lines");
    }

    @Test
    void testTimeoutEnforcement()
            throws ExecutionException, InterruptedException, TimeoutException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .build();

        // Ask for something that takes a while, with a very short timeout
        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("Write a 5000 word essay about the history of computing", context)
                .streamJson(false)
                .maxTurns(50)
                .build();

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(5), null);

        ClaudeCodeResult result = subprocess.execute()
                .get(30, TimeUnit.SECONDS);

        assertFalse(result.isSuccess(), "Should have timed out");
        assertEquals(124, result.exitCode(), "Timeout exit code should be 124");
    }

    @Test
    void testCancellation() throws InterruptedException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .build();

        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("Write a very long essay", context)
                .streamJson(false)
                .maxTurns(50)
                .build();

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(120), null);

        var future = subprocess.execute();

        // Let it start, then kill it
        Thread.sleep(3000);
        subprocess.kill();

        ClaudeCodeResult result = future.join();
        assertFalse(result.isSuccess(), "Cancelled task should not be successful");
    }

    @Test
    void testWithAllowedTools()
            throws ExecutionException, InterruptedException, TimeoutException {
        ActorContext context = ActorContext.builder()
                .workingDirectory(tempDir)
                .allowedTools(List.of("Read", "Glob"))
                .build();

        List<String> cmd = ClaudeCodeCommandBuilder
                .fromContext("List files in the current directory", context)
                .streamJson(false)
                .maxTurns(3)
                .maxBudgetUsd(0.10)
                .build();

        ClaudeCodeSubprocess subprocess = new ClaudeCodeSubprocess(
                cmd, tempDir.toFile(), Map.of(), Duration.ofSeconds(60), null);

        ClaudeCodeResult result = subprocess.execute()
                .get(90, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertNotNull(result.result());
    }
}
