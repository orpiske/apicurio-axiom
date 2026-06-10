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

interface ReportDefinitionDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    name: string;
    content: Record<string, unknown>;
}

export function ReportDefinitionDetailModal({
    isOpen,
    onClose,
    name,
    content,
}: ReportDefinitionDetailModalProps) {
    const description = (content.description as string) || "";
    const schedule = (content.schedule as string) || "none";
    const scheduleTime = (content.scheduleTime as string) || "";
    const scheduleDayOfWeek = (content.scheduleDayOfWeek as string) || "";
    const timeWindow = (content.timeWindow as string) || "";
    const promptTemplate = (content.promptTemplate as string) || "";
    const allowedTools = (content.allowedTools as string) || "";
    const timeoutSeconds = content.timeoutSeconds as number | undefined;

    const toolsList = allowedTools
        ? allowedTools.split(",").map((t) => t.trim()).filter(Boolean)
        : [];

    const formatSchedule = () => {
        const parts = [schedule];
        if (scheduleTime) parts.push(`at ${scheduleTime}`);
        if (scheduleDayOfWeek) parts.push(`on ${scheduleDayOfWeek}`);
        return parts.join(" ");
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            variant="large"
            aria-label={`Report Definition: ${name}`}
        >
            <ModalHeader title={`Report Definition: ${name}`} />
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
                    <DescriptionListGroup>
                        <DescriptionListTerm>Schedule</DescriptionListTerm>
                        <DescriptionListDescription>
                            <Label isCompact color="blue">{formatSchedule()}</Label>
                        </DescriptionListDescription>
                    </DescriptionListGroup>
                    {timeWindow && (
                        <DescriptionListGroup>
                            <DescriptionListTerm>Time Window</DescriptionListTerm>
                            <DescriptionListDescription>{timeWindow}</DescriptionListDescription>
                        </DescriptionListGroup>
                    )}
                    {timeoutSeconds !== undefined && (
                        <DescriptionListGroup>
                            <DescriptionListTerm>Timeout</DescriptionListTerm>
                            <DescriptionListDescription>{timeoutSeconds}s</DescriptionListDescription>
                        </DescriptionListGroup>
                    )}
                    {toolsList.length > 0 && (
                        <DescriptionListGroup>
                            <DescriptionListTerm>Allowed Tools</DescriptionListTerm>
                            <DescriptionListDescription>
                                <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                                    {toolsList.map((tool) => (
                                        <Label key={tool} isCompact
                                            color={tool.startsWith("@") ? "green" : "blue"}>
                                            {tool}
                                        </Label>
                                    ))}
                                </div>
                            </DescriptionListDescription>
                        </DescriptionListGroup>
                    )}
                </DescriptionList>

                {promptTemplate && (
                    <>
                        <DescriptionList isHorizontal isCompact style={{ marginTop: 16 }}>
                            <DescriptionListGroup>
                                <DescriptionListTerm style={{ whiteSpace: "nowrap" }}>Prompt Template</DescriptionListTerm>
                                <DescriptionListDescription />
                            </DescriptionListGroup>
                        </DescriptionList>
                        <CodeEditor
                            code={promptTemplate}
                            language={Language.markdown}
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
