import { useEffect, useRef, useState } from "react";
import {
    Button,
    Content,
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
import { aiEditScript } from "../config/api";

interface ChatMessage {
    role: "user" | "assistant";
    content: string;
}

interface ScriptAiModalProps {
    isOpen: boolean;
    script: string;
    actionTypeName?: string;
    actionTypeDescription?: string;
    onApply: (script: string) => void;
    onClose: () => void;
}

/**
 * Full-screen modal for AI-assisted script editing. Left side shows the
 * current script template in a code editor. Right side is a chat interface
 * for giving instructions to the AI.
 */
export function ScriptAiModal({
    isOpen, script, actionTypeName, actionTypeDescription, onApply, onClose,
}: ScriptAiModalProps) {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState("");
    const [loading, setLoading] = useState(false);
    const [localScript, setLocalScript] = useState(script);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (isOpen) {
            setLocalScript(script);
        }
    }, [isOpen, script]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSend = () => {
        const text = input.trim();
        if (!text || loading) return;

        const userMessage: ChatMessage = { role: "user", content: text };
        setMessages((prev) => [...prev, userMessage]);
        setInput("");
        setLoading(true);

        aiEditScript({
            message: text,
            currentScript: localScript || undefined,
            actionTypeName,
            actionTypeDescription,
        })
            .then((response) => {
                setMessages((prev) => [
                    ...prev,
                    { role: "assistant", content: response.explanation || "Done." },
                ]);
                if (response.script) {
                    setLocalScript(response.script);
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
        onApply(localScript);
        onClose();
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} variant="large"
            style={{ maxWidth: "95vw", width: "95vw" }}>
            <ModalHeader title="AI-Assisted Script Editor" />
            <ModalBody style={{ padding: 0, height: "100vh", display: "flex" }}>
                <Grid style={{ width: "100%" }}>
                    <GridItem className="content" span={9}>
                        <Stack>
                            <StackItem style={{ padding: "16px", paddingBottom: "8px" }}>
                                <Title headingLevel="h4" size="md">
                                    Script Template
                                </Title>
                                <div style={{ color: "#6a6e73", fontSize: "13px", marginTop: "4px" }}>
                                    Available placeholders:{" "}
                                    <code>{"{{projectId}}"}</code>,{" "}
                                    <code>{"{{eventId}}"}</code>,{" "}
                                    <code>{"{{taskId}}"}</code>,{" "}
                                    <code>{"{{issueRef}}"}</code>,{" "}
                                    <code>{"{{repository}}"}</code>,{" "}
                                    <code>{"{{projectName}}"}</code>,{" "}
                                    <code>{"{{managerInput}}"}</code>,{" "}
                                    <code>{"{{apiBaseUrl}}"}</code>
                                </div>
                            </StackItem>
                            <StackItem isFilled style={{ paddingLeft: "16px", paddingRight: "16px" }}>
                                <CodeEditor
                                    code={localScript || ""}
                                    language={Language.shell}
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
                                overflowY: "auto",
                                padding: "16px",
                                display: "flex",
                                flexDirection: "column",
                                gap: "12px",
                            }}>
                                {messages.length === 0 && (
                                    <div style={{
                                        color: "#6a6e73", fontSize: "13px", textAlign: "center",
                                        marginTop: "32px",
                                    }}>
                                        Describe what this script should do. The AI will generate
                                        a bash script with the appropriate Axiom API calls and
                                        placeholders.
                                    </div>
                                )}
                                {messages.map((msg, i) => (
                                    <div key={i} style={{
                                        alignSelf: msg.role === "user" ? "flex-end" : "flex-start",
                                        maxWidth: "90%",
                                        padding: "8px 12px",
                                        borderRadius: "8px",
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
                                        ) : (
                                            msg.content
                                        )}
                                    </div>
                                ))}
                                {loading && (
                                    <div style={{
                                        alignSelf: "flex-start",
                                        padding: "8px 12px",
                                        borderRadius: "8px",
                                        backgroundColor: "var(--pf-t--global--background--color--secondary--default)",
                                        fontSize: "13px",
                                        color: "#6a6e73",
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
                                display: "flex",
                                gap: "8px",
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
                                    placeholder="Describe what this script should do..."
                                    rows={2}
                                    isDisabled={loading}
                                    style={{ flex: 1, resize: "none" }}
                                />
                                <Button
                                    variant="primary"
                                    onClick={handleSend}
                                    isDisabled={!input.trim() || loading}
                                    style={{ alignSelf: "flex-end" }}
                                >
                                    <PaperPlaneIcon />
                                </Button>
                            </StackItem>
                        </Stack>
                    </GridItem>
                </Grid>
            </ModalBody>
            <ModalFooter>
                <Button variant="primary" onClick={handleApply}>
                    Apply Changes
                </Button>
                <Button variant="link" onClick={onClose}>
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
}
