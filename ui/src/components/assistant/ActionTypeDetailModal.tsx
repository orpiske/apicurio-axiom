import {
    Modal,
    ModalBody,
    ModalHeader,
    DescriptionList,
    DescriptionListGroup,
    DescriptionListTerm,
    DescriptionListDescription,
    Label,
    CodeBlock,
    CodeBlockCode,
} from "@patternfly/react-core";

interface ActionTypeDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    name: string;
    content: Record<string, unknown>;
}

export function ActionTypeDetailModal({ isOpen, onClose, name, content }: ActionTypeDetailModalProps) {
    const description = (content.description as string) || "";
    const executionMode = (content.executionMode as string) || "actor";
    const userTriggerable = content.userTriggerable as boolean ?? false;
    const managerTriggerable = content.managerTriggerable as boolean ?? false;
    const emitsEvent = content.emitsEvent as boolean ?? false;
    const allowedTools = (content.allowedTools as string) || "";
    const promptTemplate = (content.promptTemplate as string) || "";
    const scriptTemplate = (content.scriptTemplate as string) || "";
    const model = (content.model as string) || "";
    const engine = (content.engine as string) || "";

    const toolsList = allowedTools
        ? allowedTools.split(",").map((t) => t.trim()).filter(Boolean)
        : [];

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            variant="large"
            aria-label={`Action Type: ${name}`}
        >
            <ModalHeader title={`Action Type: ${name}`} />
            <ModalBody>
                <DescriptionList isHorizontal>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Name</DescriptionListTerm>
                        <DescriptionListDescription>{name}</DescriptionListDescription>
                    </DescriptionListGroup>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Description</DescriptionListTerm>
                        <DescriptionListDescription>{description || "—"}</DescriptionListDescription>
                    </DescriptionListGroup>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Execution Mode</DescriptionListTerm>
                        <DescriptionListDescription>
                            <Label isCompact color={executionMode === "actor" ? "blue" : "orange"}>
                                {executionMode}
                            </Label>
                        </DescriptionListDescription>
                    </DescriptionListGroup>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Flags</DescriptionListTerm>
                        <DescriptionListDescription>
                            {userTriggerable && <Label isCompact color="green" style={{ marginRight: 4 }}>User Triggerable</Label>}
                            {managerTriggerable && <Label isCompact color="blue" style={{ marginRight: 4 }}>Manager Triggerable</Label>}
                            {emitsEvent && <Label isCompact color="purple" style={{ marginRight: 4 }}>Emits Event</Label>}
                            {!userTriggerable && !managerTriggerable && !emitsEvent && "—"}
                        </DescriptionListDescription>
                    </DescriptionListGroup>
                </DescriptionList>

                {toolsList.length > 0 && (
                    <>
                        <h4 style={{ marginTop: 16, marginBottom: 8 }}>Allowed Tools</h4>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                            {toolsList.map((tool) => (
                                <Label key={tool} isCompact
                                    color={tool.startsWith("@") ? "green" : "blue"}>
                                    {tool}
                                </Label>
                            ))}
                        </div>
                    </>
                )}

                {promptTemplate && (
                    <>
                        <h4 style={{ marginTop: 16, marginBottom: 8 }}>Prompt Template</h4>
                        <CodeBlock>
                            <CodeBlockCode>{promptTemplate}</CodeBlockCode>
                        </CodeBlock>
                    </>
                )}

                {scriptTemplate && (
                    <>
                        <h4 style={{ marginTop: 16, marginBottom: 8 }}>Script Template</h4>
                        <CodeBlock>
                            <CodeBlockCode>{scriptTemplate}</CodeBlockCode>
                        </CodeBlock>
                    </>
                )}

                {(model || engine) && (
                    <>
                        <h4 style={{ marginTop: 16, marginBottom: 8 }}>Advanced</h4>
                        <DescriptionList isHorizontal isCompact>
                            {model && (
                                <DescriptionListGroup>
                                    <DescriptionListTerm>Model</DescriptionListTerm>
                                    <DescriptionListDescription>{model}</DescriptionListDescription>
                                </DescriptionListGroup>
                            )}
                            {engine && (
                                <DescriptionListGroup>
                                    <DescriptionListTerm>Engine</DescriptionListTerm>
                                    <DescriptionListDescription>{engine}</DescriptionListDescription>
                                </DescriptionListGroup>
                            )}
                        </DescriptionList>
                    </>
                )}
            </ModalBody>
        </Modal>
    );
}
