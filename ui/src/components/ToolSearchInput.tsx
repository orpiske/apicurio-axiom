import { useEffect, useRef, useState, useCallback } from "react";
import {
    Menu,
    MenuContent,
    MenuItem,
    MenuList,
    Popper,
    SearchInput,
} from "@patternfly/react-core";
import {
    fetchToolsets, type Toolset,
    fetchTools, type ToolDefinition,
    fetchMcpServers, type McpServer,
} from "../config/api";

/**
 * Well-known Claude Code built-in tools available for autocomplete.
 */
const BUILTIN_TOOLS = [
    "Read",
    "Glob",
    "Grep",
    "Edit",
    "Write",
    "Bash(ls *)",
    "Bash(cat *)",
    "Bash(head *)",
    "Bash(tail *)",
    "Bash(find *)",
    "Bash(wc *)",
    "Bash(file *)",
    "Bash(git log *)",
    "Bash(git diff *)",
    "Bash(git show *)",
    "Bash(git status *)",
    "Bash(git branch *)",
    "Bash(git add *)",
    "Bash(git commit *)",
    "Bash(git checkout *)",
    "Bash(git switch *)",
    "Bash(git push *)",
    "Bash(git merge *)",
    "Bash(gh issue *)",
    "Bash(gh pr *)",
    "Bash(gh api *)",
    "Bash(gh repo *)",
    "Bash(date *)",
    "Bash(mkdir *)",
    "Bash(cp *)",
    "Bash(mv *)",
];

interface ToolSearchInputProps {
    /** Called when a tool is selected (either from suggestions or freeform). */
    onAdd: (tool: string) => void;
    /** Tools already in the list, used to filter out duplicates from suggestions. */
    existingTools?: string[];
    /** Placeholder text for the input. */
    placeholder?: string;
}

/**
 * A typeahead search input for adding tools to an allowed tools list.
 * Provides autocomplete suggestions from:
 * - Well-known Claude Code built-in tools
 * - Toolset references (prefixed with @)
 * - MCP tool names from configured tools
 *
 * Uses the PatternFly SearchInput + Menu + Popper composition pattern.
 */
export function ToolSearchInput({
    onAdd,
    existingTools = [],
    placeholder = "Type a tool name or @ToolsetName and press Enter",
}: ToolSearchInputProps) {
    const [value, setValue] = useState("");
    const [hint, setHint] = useState("");
    const [isOpen, setIsOpen] = useState(false);
    const [filteredItems, setFilteredItems] = useState<string[]>([]);
    const [toolsets, setToolsets] = useState<Toolset[]>([]);
    const [customTools, setCustomTools] = useState<ToolDefinition[]>([]);
    const [mcpServers, setMcpServers] = useState<McpServer[]>([]);

    const searchInputRef = useRef<HTMLInputElement>(null);
    const autocompleteRef = useRef<HTMLDivElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    // Load toolsets, custom tools, and MCP servers once on mount
    useEffect(() => {
        Promise.all([fetchToolsets(), fetchTools(), fetchMcpServers()])
            .then(([ts, tools, servers]) => {
                setToolsets(ts);
                setCustomTools(tools);
                setMcpServers(servers);
            })
            .catch(console.error);
    }, []);

    // Build the full suggestion list (toolset refs + MCP tools + builtins)
    const allSuggestions = useCallback((): string[] => {
        const toolsetRefs = toolsets.map((ts) => `@${ts.name}`);

        // Custom script tools exposed via the built-in axiom-tools MCP server
        const mcpToolNames = customTools.map((t) => `mcp__axiom-tools__${t.name}`);

        // External MCP server tools use mcp__<serverName>__* wildcard patterns
        const mcpServerPatterns = mcpServers.map((s) => `mcp__${s.name}__*`);

        return [...toolsetRefs, ...mcpToolNames, ...mcpServerPatterns, ...BUILTIN_TOOLS];
    }, [toolsets, customTools, mcpServers]);

    // Filter suggestions whenever value changes
    useEffect(() => {
        if (!value.trim()) {
            setFilteredItems([]);
            setIsOpen(false);
            setHint("");
            return;
        }

        const lower = value.toLowerCase();
        const existing = new Set(existingTools.map((t) => t.toLowerCase()));
        const filtered = allSuggestions().filter(
            (item) =>
                item.toLowerCase().includes(lower) &&
                !existing.has(item.toLowerCase())
        );

        setFilteredItems(filtered.slice(0, 12));
        setIsOpen(filtered.length > 0);

        // Set hint to the first item that starts with the input
        const startsWithMatch = filtered.find((item) =>
            item.toLowerCase().startsWith(lower)
        );
        if (startsWithMatch) {
            setHint(value + startsWithMatch.substring(value.length));
        } else {
            setHint("");
        }
    }, [value, existingTools, allSuggestions]);

    // Close menu when clicking outside
    useEffect(() => {
        const handleClick = (event: MouseEvent) => {
            if (
                isOpen &&
                !containerRef.current?.contains(event.target as Node)
            ) {
                setIsOpen(false);
            }
        };
        document.addEventListener("click", handleClick);
        return () => document.removeEventListener("click", handleClick);
    }, [isOpen]);

    const doAdd = (tool: string) => {
        const trimmed = tool.trim();
        if (trimmed) {
            onAdd(trimmed);
            setValue("");
            setHint("");
            setIsOpen(false);
        }
    };

    const onChange = (
        _event: React.FormEvent<HTMLInputElement>,
        newValue: string
    ) => {
        setValue(newValue);
    };

    const onClear = () => {
        setValue("");
        setHint("");
        setIsOpen(false);
    };

    const onSelect = (
        _event: React.MouseEvent | undefined,
        itemId: string | number | undefined
    ) => {
        doAdd(itemId as string);
        searchInputRef.current?.focus();
    };

    const handleKeyDown = (
        event: React.KeyboardEvent<HTMLInputElement>
    ) => {
        switch (event.key) {
            case "Enter":
                event.preventDefault();
                if (hint && filteredItems.length === 1) {
                    // Accept the single match
                    doAdd(hint);
                } else {
                    // Add whatever the user typed (freeform)
                    doAdd(value);
                }
                break;
            case "Tab":
                if (hint) {
                    event.preventDefault();
                    setValue(hint);
                    setHint("");
                    setIsOpen(false);
                }
                break;
            case "Escape":
                setIsOpen(false);
                break;
            case "ArrowDown":
                if (isOpen) {
                    event.preventDefault();
                    const firstMenuItem =
                        autocompleteRef.current?.querySelector<HTMLElement>(
                            '[role="menuitem"]'
                        );
                    firstMenuItem?.focus();
                }
                break;
        }
    };

    const autocomplete = (
        <Menu ref={autocompleteRef} onSelect={onSelect} isScrollable>
            <MenuContent maxMenuHeight="200px">
                <MenuList>
                    {filteredItems.map((item) => (
                        <MenuItem
                            key={item}
                            itemId={item}
                            style={
                                item.startsWith("@")
                                    ? {
                                          color: "var(--pf-t--global--color--brand--default)",
                                          fontWeight: 500,
                                      }
                                    : item.startsWith("mcp__")
                                      ? { fontFamily: "var(--pf-t--global--font--family--mono)" }
                                      : undefined
                            }
                        >
                            {item}
                        </MenuItem>
                    ))}
                </MenuList>
            </MenuContent>
        </Menu>
    );

    return (
        <div ref={containerRef}>
            <Popper
                trigger={
                    <SearchInput
                        ref={searchInputRef}
                        placeholder={placeholder}
                        value={value}
                        hint={hint}
                        onChange={onChange}
                        onClear={onClear}
                        onKeyDown={handleKeyDown}
                        aria-label="Add tool"
                    />
                }
                popper={autocomplete}
                isVisible={isOpen}
                enableFlip
                appendTo="inline"
            />
        </div>
    );
}
