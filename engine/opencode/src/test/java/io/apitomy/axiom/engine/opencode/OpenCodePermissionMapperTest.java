package io.apitomy.axiom.engine.opencode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodePermissionMapper} — verifies correct translation
 * of Axiom tool names to OpenCode permission configuration format.
 */
class OpenCodePermissionMapperTest {

    // --- Simple tool mappings ---

    @Test
    void testSimpleToolsMapping() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "Glob", "Grep"), null);

        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("glob"));
        assertEquals("allow", result.get("grep"));
        assertEquals(3, result.size());
    }

    @Test
    void testWriteAndEditMapToEditPermission() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Write", "Edit"), null);

        assertEquals("allow", result.get("edit"));
        // Both map to "edit", so only one entry
        assertEquals(1, result.size());
    }

    // --- Bash tool mappings ---

    @Test
    void testUnrestrictedBash() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "Bash"), null);

        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("bash"));
    }

    @Test
    void testParameterizedBashPatterns() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Bash(git log *)", "Bash(gh issue *)"), null);

        // Bash should be an object with deny-by-default + specific patterns
        assertTrue(result.get("bash") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> bashConfig = (Map<String, String>) result.get("bash");
        assertEquals("deny", bashConfig.get("*"));
        assertEquals("allow", bashConfig.get("git log *"));
        assertEquals("allow", bashConfig.get("gh issue *"));
        assertEquals(3, bashConfig.size());
    }

    @Test
    void testMixedBashPatternsWithUnrestricted() {
        // When both unrestricted Bash and Bash(pattern) are present,
        // unrestricted wins (simple "allow")
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Bash", "Bash(git log *)"), null);

        assertEquals("allow", result.get("bash"));
    }

    // --- MCP tool mappings ---

    @Test
    void testMcpToolNameTranslation() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("mcp__axiom-tools__list_github_issues",
                        "mcp__axiom-tools__list_github_prs"),
                null);

        assertEquals("allow", result.get("axiom-tools_list_github_issues"));
        assertEquals("allow", result.get("axiom-tools_list_github_prs"));
    }

    @Test
    void testMcpToolNameMapping() {
        assertEquals("axiom-tools_list_issues",
                OpenCodePermissionMapper.mapMcpToolName("mcp__axiom-tools__list_issues"));
        assertEquals("myserver_search",
                OpenCodePermissionMapper.mapMcpToolName("mcp__myserver__search"));
    }

    @Test
    void testMcpToolNameReverseMapping() {
        assertEquals("mcp__axiom-tools__list_issues",
                OpenCodePermissionMapper.toAxiomMcpToolName("axiom-tools_list_issues", "axiom-tools"));
        assertEquals("mcp__myserver__search",
                OpenCodePermissionMapper.toAxiomMcpToolName("myserver_search", "myserver"));
    }

    // --- StructuredOutput ---

    @Test
    void testStructuredOutputIsIgnored() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("StructuredOutput"), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testStructuredOutputWithOtherTools() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "StructuredOutput", "Glob"), null);

        assertEquals(2, result.size());
        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("glob"));
        assertNull(result.get("structuredoutput"));
    }

    // --- Disallowed tools ---

    @Test
    void testDisallowedTools() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "Bash", "Edit"),
                List.of("Write"));

        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("bash"));
        // Edit was allowed, then Write (also maps to "edit") is denied — last wins
        assertEquals("deny", result.get("edit"));
    }

    @Test
    void testDisallowedMcpTool() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read"),
                List.of("mcp__myserver__dangerous_tool"));

        assertEquals("allow", result.get("read"));
        assertEquals("deny", result.get("myserver_dangerous_tool"));
    }

    // --- Empty / null inputs ---

    @Test
    void testNullAllowedTools() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testEmptyAllowedTools() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(List.of(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testOnlyDisallowedTools() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                null, List.of("Bash", "Edit"));

        assertEquals("deny", result.get("bash"));
        assertEquals("deny", result.get("edit"));
    }

    // --- Comprehensive / realistic scenarios ---

    @Test
    void testRealisticActionTypeTools() {
        // A typical action type's allowed tools list
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "Glob", "Grep",
                        "Bash(ls *)", "Bash(cat *)", "Bash(head *)", "Bash(tail *)",
                        "Bash(find *)", "Bash(wc *)", "Bash(file *)",
                        "Bash(gh issue *)", "Bash(gh pr *)", "Bash(gh api *)",
                        "Bash(gh repo *)", "Bash(date *)",
                        "mcp__axiom-tools__list_github_issues",
                        "mcp__axiom-tools__list_github_prs"),
                null);

        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("glob"));
        assertEquals("allow", result.get("grep"));

        // Bash should be object with deny-default + patterns
        assertTrue(result.get("bash") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> bashConfig = (Map<String, String>) result.get("bash");
        assertEquals("deny", bashConfig.get("*"));
        assertEquals("allow", bashConfig.get("ls *"));
        assertEquals("allow", bashConfig.get("gh issue *"));
        assertEquals("allow", bashConfig.get("date *"));

        // MCP tools
        assertEquals("allow", result.get("axiom-tools_list_github_issues"));
        assertEquals("allow", result.get("axiom-tools_list_github_prs"));
    }

    @Test
    void testUnknownToolPassedThrough() {
        Map<String, Object> result = OpenCodePermissionMapper.mapPermissions(
                List.of("Read", "CustomTool"), null);

        assertEquals("allow", result.get("read"));
        assertEquals("allow", result.get("customtool"));
    }

    // --- mapMcpToolName edge cases ---

    @Test
    void testMapMcpToolNameNull() {
        assertEquals("", OpenCodePermissionMapper.mapMcpToolName(null));
    }

    @Test
    void testMapMcpToolNameNonMcp() {
        assertEquals("Read", OpenCodePermissionMapper.mapMcpToolName("Read"));
    }

    @Test
    void testToAxiomMcpToolNameNull() {
        assertNull(OpenCodePermissionMapper.toAxiomMcpToolName(null, "server"));
        assertEquals("tool", OpenCodePermissionMapper.toAxiomMcpToolName("tool", null));
    }
}
