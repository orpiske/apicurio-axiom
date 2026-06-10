import { useState } from "react";
import {
    ExpandableSection,
    Label,
} from "@patternfly/react-core";
import { Light as SyntaxHighlighter } from "react-syntax-highlighter";
import json from "react-syntax-highlighter/dist/esm/languages/hljs/json";
import bash from "react-syntax-highlighter/dist/esm/languages/hljs/bash";
import { stackoverflowLight } from "react-syntax-highlighter/dist/esm/styles/hljs";

SyntaxHighlighter.registerLanguage("json", json);
SyntaxHighlighter.registerLanguage("bash", bash);

interface AssistantToolUseBlockProps {
    toolName: string;
    input?: Record<string, unknown>;
    result?: string;
    isError?: boolean;
}

export function AssistantToolUseBlock({ toolName, input, result, isError }: AssistantToolUseBlockProps) {
    const [isExpanded, setIsExpanded] = useState(false);

    const inputPreview = input
        ? JSON.stringify(input).substring(0, 100)
        : "";

    const codeStyle = {
        margin: 0,
        borderRadius: "4px",
        fontSize: "12px",
        maxHeight: "250px",
        overflow: "auto",
    };

    return (
        <div style={{
            margin: "4px 0",
            padding: "8px 12px",
            borderRadius: "6px",
            backgroundColor: "#f0f0f0",
            fontSize: "13px",
        }}>
            <ExpandableSection
                toggleText={
                    <span>
                        <Label isCompact color={isError ? "red" : "blue"}>
                            {toolName}
                        </Label>
                        {inputPreview && (
                            <span style={{ marginLeft: 8, color: "#6a6e73", fontSize: "12px" }}>
                                {inputPreview}{input && JSON.stringify(input).length > 100 ? "..." : ""}
                            </span>
                        )}
                    </span>
                }
                isExpanded={isExpanded}
                onToggle={(_e, expanded) => setIsExpanded(expanded)}
                isIndented
            >
                {input && (
                    <div style={{ marginBottom: result ? 8 : 0 }}>
                        <SyntaxHighlighter
                            language="json"
                            style={stackoverflowLight}
                            customStyle={codeStyle}
                            wrapLongLines
                        >
                            {JSON.stringify(input, null, 2)}
                        </SyntaxHighlighter>
                    </div>
                )}
                {result && (
                    <SyntaxHighlighter
                        language={isJson(result) ? "json" : "bash"}
                        style={stackoverflowLight}
                        customStyle={{
                            ...codeStyle,
                            ...(isError ? { backgroundColor: "#fef3f2" } : {}),
                        }}
                        wrapLongLines
                    >
                        {isJson(result) ? formatJson(result) : result}
                    </SyntaxHighlighter>
                )}
            </ExpandableSection>
        </div>
    );
}

function isJson(text: string): boolean {
    const trimmed = text.trim();
    return (trimmed.startsWith("{") || trimmed.startsWith("[")) && tryParseJson(trimmed) !== null;
}

function tryParseJson(text: string): unknown {
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

function formatJson(text: string): string {
    try {
        return JSON.stringify(JSON.parse(text), null, 2);
    } catch {
        return text;
    }
}
