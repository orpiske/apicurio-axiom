import { useEffect, useRef, useState } from "react";
import {
    Button,
    Content,
    Flex,
    FlexItem,
    Grid,
    GridItem,
    Modal,
    ModalBody,
    ModalFooter,
    ModalHeader,
    Spinner,
    Stack,
    StackItem,
    TextArea,
    Title,
} from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import PaperPlaneIcon from "@patternfly/react-icons/dist/esm/icons/paper-plane-icon";
import { aiEditActionPrompt } from "../config/api";

interface ChatMessage {
    role: "user" | "assistant";
    content: string;
}

interface ActionTypeAiModalProps {
    isOpen: boolean;
    promptTemplate: string;
    allowedTools: string[];
    actionTypeName?: string;
    actionTypeDescription?: string;
    onApply: (promptTemplate: string, allowedTools: string[]) => void;
    onClose: () => void;
}

/**
 * Full-screen modal for AI-assisted action type editing. Left side shows
 * allowed tools and the prompt template preview. Right side is a chat
 * interface for giving instructions to the AI.
 */
export function ActionTypeAiModal({
    isOpen, promptTemplate, allowedTools, actionTypeName, actionTypeDescription,
    onApply, onClose,
}: ActionTypeAiModalProps) {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState("");
    const [loading, setLoading] = useState(false);
    const [localPrompt, setLocalPrompt] = useState(promptTemplate);
    const [localTools, setLocalTools] = useState(allowedTools);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (isOpen) {
            setLocalPrompt(promptTemplate);
            setLocalTools(allowedTools);
        }
    }, [isOpen, promptTemplate, allowedTools]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSend = () => {
        const text = input.trim();
        if (!text || loading) return;

        setMessages((prev) => [...prev, { role: "user", content: text }]);
        setInput("");
        setLoading(true);

        aiEditActionPrompt({
            message: text,
            currentPromptTemplate: localPrompt || undefined,
            currentAllowedTools: localTools.length > 0 ? localTools : undefined,
            reportName: actionTypeName,
            reportDescription: actionTypeDescription,
        })
            .then((response) => {
                setMessages((prev) => [
                    ...prev,
                    { role: "assistant", content: response.explanation || "Done." },
                ]);
                if (response.promptTemplate) {
                    setLocalPrompt(response.promptTemplate);
                }
                if (Array.isArray(response.allowedTools)) {
                    setLocalTools(response.allowedTools);
                }
            })
            .catch((err) => {
                setMessages((prev) => [
                    ...prev,
                    { role: "assistant", content: "Error: " + err.message },
                ]);
            })
            .finally(() => setLoading(false));
    };

    const handleApply = () => {
        onApply(localPrompt, localTools);
        onClose();
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} variant="large"
            style={{ maxWidth: "95vw", width: "95vw" }}>
            <ModalHeader title="AI-Assisted Action Type Editor" />
            <ModalBody style={{ padding: 0, height: "100vh", display: "flex" }}>
                <Grid style={{ width: "100%" }}>
                    <GridItem className="content" span={9}>
                        <Stack>
                            <StackItem style={{ padding: "16px", paddingBottom: "8px" }}>
                                <Title headingLevel="h4" size="md" style={{ marginBottom: "8px" }}>
                                    Allowed Tools ({localTools.length})
                                </Title>
                                {localTools.length > 0 ? (
                                    <div style={{ display: "flex", flexWrap: "wrap", gap: "4px",
                                        maxHeight: "80px", overflowY: "auto" }}>
                                        {localTools.map((tool) => (
                                            <Flex key={tool}
                                                alignItems={{ default: "alignItemsCenter" }}
                                                style={{
                                                    padding: "4px 8px",
                                                    backgroundColor: tool.startsWith("@")
                                                        ? "var(--pf-t--global--background--color--primary--default)"
                                                        : "var(--pf-t--global--background--color--secondary--default)",
                                                    borderRadius: "4px",
                                                    border: tool.startsWith("@")
                                                        ? "1px solid var(--pf-t--global--border--color--default)"
                                                        : "none",
                                                }}>
                                                <FlexItem>
                                                    <code style={{
                                                        fontSize: "12px",
                                                        color: tool.startsWith("@")
                                                            ? "var(--pf-t--global--color--brand--default)"
                                                            : "inherit",
                                                    }}>{tool}</code>
                                                </FlexItem>
                                            </Flex>
                                        ))}
                                    </div>
                                ) : (
                                    <div style={{ color: "#6a6e73", fontSize: "13px" }}>
                                        No tools configured. Ask the AI to recommend tools.
                                    </div>
                                )}
                            </StackItem>
                            <StackItem style={{ paddingLeft: "16px", paddingBottom: "4px" }}>
                                <Title headingLevel="h4" size="md">
                                    Prompt Template
                                </Title>
                                <div style={{ color: "#6a6e73", fontSize: "13px", marginTop: "2px" }}>
                                    Placeholders:{" "}
                                    <code>{"{{managerInput}}"}</code>,{" "}
                                    <code>{"{{issueRef}}"}</code>,{" "}
                                    <code>{"{{repository}}"}</code>,{" "}
                                    <code>{"{{projectName}}"}</code>
                                </div>
                            </StackItem>
                            <StackItem isFilled style={{ paddingLeft: "16px", paddingRight: "16px" }}>
                                <CodeEditor
                                    code={localPrompt || ""}
                                    language={Language.markdown}
                                    isFullHeight
                                    isReadOnly={false}
                                    isLineNumbersVisible
                                />
                            </StackItem>
                        </Stack>
                    </GridItem>
                    <GridItem className="chat" span={3} style={{
                        borderLeft: "1px solid var(--pf-t--global--border--color--default)",
                    }}>
                        <Stack>
                            <StackItem isFilled style={{
                                overflowY: "auto", padding: "16px",
                                display: "flex", flexDirection: "column", gap: "12px",
                            }}>
                                {messages.length === 0 && (
                                    <div style={{
                                        color: "#6a6e73", fontSize: "13px", textAlign: "center",
                                        marginTop: "32px",
                                    }}>
                                        Describe what this action type should do. The AI will
                                        generate a prompt template and recommend the appropriate
                                        tools.
                                    </div>
                                )}
                                {messages.map((msg, i) => (
                                    <div key={i} style={{
                                        alignSelf: msg.role === "user" ? "flex-end" : "flex-start",
                                        maxWidth: "90%", padding: "8px 12px", borderRadius: "8px",
                                        backgroundColor: msg.role === "user"
                                            ? "var(--pf-t--global--color--brand--default)"
                                            : "var(--pf-t--global--background--color--secondary--default)",
                                        color: msg.role === "user" ? "white" : "inherit",
                                        fontSize: "13px",
                                    }}>
                                        {msg.role === "assistant" ? (
                                            <Content>
                                                <Markdown remarkPlugins={[remarkGfm]}>{msg.content}</Markdown>
                                            </Content>
                                        ) : msg.content}
                                    </div>
                                ))}
                                {loading && (
                                    <div style={{
                                        alignSelf: "flex-start", padding: "8px 12px", borderRadius: "8px",
                                        backgroundColor: "var(--pf-t--global--background--color--secondary--default)",
                                        fontSize: "13px", color: "#6a6e73",
                                    }}>
                                        <Spinner size="sm" style={{ marginRight: "5px" }} />
                                        Thinking...
                                    </div>
                                )}
                                <div ref={messagesEndRef} />
                            </StackItem>
                            <StackItem style={{
                                padding: "6px 8px",
                                borderTop: "1px solid var(--pf-t--global--border--color--default)",
                                display: "flex", gap: "8px",
                            }}>
                                <TextArea
                                    value={input}
                                    onChange={(_e, v) => setInput(v)}
                                    onKeyDown={(e) => {
                                        if (e.key === "Enter" && !e.shiftKey) {
                                            e.preventDefault();
                                            handleSend();
                                        }
                                    }}
                                    placeholder="Describe what this action type should do..."
                                    rows={2}
                                    isDisabled={loading}
                                    style={{ flex: 1, resize: "none" }}
                                />
                                <Button variant="primary" onClick={handleSend}
                                    isDisabled={!input.trim() || loading}
                                    style={{ alignSelf: "flex-end" }}>
                                    <PaperPlaneIcon />
                                </Button>
                            </StackItem>
                        </Stack>
                    </GridItem>
                </Grid>
            </ModalBody>
            <ModalFooter>
                <Button variant="primary" onClick={handleApply}>Apply Changes</Button>
                <Button variant="link" onClick={onClose}>Cancel</Button>
            </ModalFooter>
        </Modal>
    );
}
