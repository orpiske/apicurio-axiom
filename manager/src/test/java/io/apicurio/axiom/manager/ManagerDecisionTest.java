package io.apicurio.axiom.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ManagerDecision record.
 */
class ManagerDecisionTest {

    @Test
    void testCreateTaskDecision() {
        ManagerDecision decision = new ManagerDecision(
                "create_task", "analyze", "claude-agent",
                "Analyze this issue", 0.9, "New issue needs analysis");

        assertTrue(decision.isCreateTask());
        assertFalse(decision.isIgnore());
        assertFalse(decision.isScriptAction());
        assertFalse(decision.isEscalate());
        assertEquals("analyze", decision.actionType());
        assertEquals("claude-agent", decision.actorHint());
        assertEquals(0.9, decision.confidence());
    }

    @Test
    void testIgnoreDecision() {
        ManagerDecision decision = new ManagerDecision(
                "ignore", null, null, null, 0.95, "Bot comment, ignoring");

        assertFalse(decision.isCreateTask());
        assertTrue(decision.isIgnore());
        assertNull(decision.actionType());
    }

    @Test
    void testScriptActionDecision() {
        ManagerDecision decision = new ManagerDecision(
                "script_action", "close-project", null, null, 0.85, "Issue closed");

        assertTrue(decision.isScriptAction());
        assertEquals("close-project", decision.actionType());
    }

    @Test
    void testEscalateDecision() {
        ManagerDecision decision = new ManagerDecision(
                "escalate", null, null, null, 0.3, "Uncertain what to do");

        assertTrue(decision.isEscalate());
        assertEquals(0.3, decision.confidence());
    }
}
