package io.apicurio.axiom.actors.claudecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClaudeCodeResult parsing and construction.
 */
class ClaudeCodeResultTest {

    @Test
    void testSuccessfulResult() {
        ClaudeCodeResult result = new ClaudeCodeResult(
                "Analysis complete", "session-123", 0.05, 1200L, 800L, 0, null);

        assertTrue(result.isSuccess());
        assertEquals("Analysis complete", result.result());
        assertEquals("session-123", result.sessionId());
        assertEquals(0.05, result.totalCostUsd());
        assertEquals(1200L, result.inputTokens());
        assertEquals(800L, result.outputTokens());
        assertEquals(0, result.exitCode());
    }

    @Test
    void testFailedResult() {
        ClaudeCodeResult result = ClaudeCodeResult.failed("Process crashed", 1);

        assertFalse(result.isSuccess());
        assertEquals("Process crashed", result.result());
        assertNull(result.sessionId());
        assertNull(result.totalCostUsd());
        assertEquals(1, result.exitCode());
    }

    @Test
    void testTimeoutResult() {
        ClaudeCodeResult result = ClaudeCodeResult.failed("Timed out", 124);

        assertFalse(result.isSuccess());
        assertEquals(124, result.exitCode());
    }
}
