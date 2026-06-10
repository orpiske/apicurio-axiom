import {
    Modal,
    ModalBody,
    ModalHeader,
    DescriptionList,
    DescriptionListGroup,
    DescriptionListTerm,
    DescriptionListDescription,
    Label,
} from "@patternfly/react-core";
import { CodeEditor, Language } from "@patternfly/react-code-editor";

interface ToolParam {
    name: string;
    type?: string;
    description?: string;
    required?: boolean;
}

interface ToolDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    name: string;
    content: Record<string, unknown>;
}

export function ToolDetailModal({ isOpen, onClose, name, content }: ToolDetailModalProps) {
    const description = (content.description as string) || "";
    const parameters = (content.parameters as ToolParam[]) || [];
    const scriptTemplate = (content.scriptTemplate as string) || "";
    const labels = (content.labels as string[]) || [];

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            variant="large"
            aria-label={`Tool: ${name}`}
        >
            <ModalHeader title={`Tool: ${name}`} />
            <ModalBody>
                <DescriptionList isHorizontal isCompact>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Name</DescriptionListTerm>
                        <DescriptionListDescription>{name}</DescriptionListDescription>
                    </DescriptionListGroup>
                    <DescriptionListGroup>
                        <DescriptionListTerm>Description</DescriptionListTerm>
                        <DescriptionListDescription>{description || "—"}</DescriptionListDescription>
                    </DescriptionListGroup>
                    {labels.length > 0 && (
                        <DescriptionListGroup>
                            <DescriptionListTerm>Labels</DescriptionListTerm>
                            <DescriptionListDescription>
                                {labels.map((l) => (
                                    <Label key={l} isCompact style={{ marginRight: 4 }}>
                                        {l}
                                    </Label>
                                ))}
                            </DescriptionListDescription>
                        </DescriptionListGroup>
                    )}
                </DescriptionList>

                {parameters.length > 0 && (
                    <>
                        <DescriptionList isHorizontal isCompact style={{ marginTop: 16 }}>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Parameters</DescriptionListTerm>
                                <DescriptionListDescription />
                            </DescriptionListGroup>
                        </DescriptionList>
                        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
                            <thead>
                                <tr style={{ borderBottom: "2px solid #d2d2d2" }}>
                                    <th style={{ textAlign: "left", padding: "6px 8px" }}>Name</th>
                                    <th style={{ textAlign: "left", padding: "6px 8px" }}>Type</th>
                                    <th style={{ textAlign: "left", padding: "6px 8px" }}>Description</th>
                                    <th style={{ textAlign: "left", padding: "6px 8px" }}>Required</th>
                                </tr>
                            </thead>
                            <tbody>
                                {parameters.map((p) => (
                                    <tr key={p.name} style={{ borderBottom: "1px solid #d2d2d2" }}>
                                        <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>{p.name}</td>
                                        <td style={{ padding: "6px 8px" }}>{p.type || "string"}</td>
                                        <td style={{ padding: "6px 8px" }}>{p.description || "—"}</td>
                                        <td style={{ padding: "6px 8px" }}>{p.required ? "Yes" : "No"}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </>
                )}

                {scriptTemplate && (
                    <>
                        <DescriptionList isHorizontal isCompact style={{ marginTop: 16 }}>
                            <DescriptionListGroup>
                                <DescriptionListTerm style={{ whiteSpace: "nowrap" }}>Script Template</DescriptionListTerm>
                                <DescriptionListDescription />
                            </DescriptionListGroup>
                        </DescriptionList>
                        <CodeEditor
                            code={scriptTemplate}
                            language={Language.shell}
                            height="300px"
                            isReadOnly
                            isLineNumbersVisible
                        />
                    </>
                )}
            </ModalBody>
        </Modal>
    );
}
