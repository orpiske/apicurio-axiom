import { useState, useEffect, useCallback } from "react";
import {
    Button,
    EmptyState,
    EmptyStateBody,
    Flex,
    FlexItem,
    Label,
    PageSection,
    Pagination,
    Tab,
    TabContent,
    TabTitleText,
    Tabs,
    Title,
    Toolbar,
    ToolbarContent,
    ToolbarItem,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import SaveIcon from "@patternfly/react-icons/dist/esm/icons/save-icon";
import SyncAltIcon from "@patternfly/react-icons/dist/esm/icons/sync-alt-icon";
import {
    type ManagerConfig,
    type ActivityLogEntry,
    fetchManagerConfig,
    updateManagerConfig,
    fetchActivityLog,
} from "../config/api";
import { ExecutionLogModal } from "../components/ExecutionLogModal";

const MANAGER_ENTRY_TYPES = "manager-evaluated,manager-error,manager-skipped,manager-escalation,manager-no-decision";

const ENTRY_TYPE_COLORS: Record<string, "blue" | "green" | "orange" | "grey" | "red"> = {
    "manager-evaluated": "blue",
    "manager-error": "red",
    "manager-skipped": "grey",
    "manager-escalation": "orange",
    "manager-no-decision": "grey",
};

export function ManagerConfigPage() {
    const [config, setConfig] = useState<ManagerConfig>({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [dirty, setDirty] = useState(false);
    const [activeTab, setActiveTab] = useState(0);

    // Event history state
    const [events, setEvents] = useState<ActivityLogEntry[]>([]);
    const [eventsTotalCount, setEventsTotalCount] = useState(0);
    const [eventsPage, setEventsPage] = useState(1);
    const [eventsPerPage, setEventsPerPage] = useState(20);
    const [eventsLoading, setEventsLoading] = useState(false);

    // Log modal state
    const [isLogModalOpen, setIsLogModalOpen] = useState(false);
    const [logActivityId, setLogActivityId] = useState<number | null>(null);

    const loadConfig = useCallback(() => {
        setLoading(true);
        fetchManagerConfig()
            .then((c) => { setConfig(c); setDirty(false); })
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    const loadEvents = useCallback(() => {
        setEventsLoading(true);
        fetchActivityLog(eventsPage, eventsPerPage, undefined, undefined, undefined, MANAGER_ENTRY_TYPES)
            .then((results) => {
                setEvents(results.items);
                setEventsTotalCount(results.totalCount);
            })
            .catch(console.error)
            .finally(() => setEventsLoading(false));
    }, [eventsPage, eventsPerPage]);

    useEffect(() => { loadConfig(); }, [loadConfig]);
    useEffect(() => { loadEvents(); }, [loadEvents]);

    const handleSave = () => {
        setSaving(true);
        updateManagerConfig(config)
            .then((c) => { setConfig(c); setDirty(false); })
            .catch(console.error)
            .finally(() => setSaving(false));
    };

    const handleViewLog = (activityId: number) => {
        setLogActivityId(activityId);
        setIsLogModalOpen(true);
    };

    if (loading) {
        return (
            <PageSection>
                <EmptyState><EmptyStateBody>Loading manager configuration...</EmptyStateBody></EmptyState>
            </PageSection>
        );
    }

    return (
        <PageSection>
            <Flex
                justifyContent={{ default: "justifyContentSpaceBetween" }}
                alignItems={{ default: "alignItemsCenter" }}
                style={{ marginBottom: "16px" }}
            >
                <FlexItem>
                    <Title headingLevel="h1" size="lg">Manager Configuration</Title>
                </FlexItem>
                <FlexItem>
                    <Button
                        variant="primary" icon={<SaveIcon />}
                        onClick={handleSave}
                        isDisabled={!dirty || saving}
                        isLoading={saving}
                    >
                        {saving ? "Saving..." : "Save Changes"}
                    </Button>
                </FlexItem>
            </Flex>

            <Tabs activeKey={activeTab} onSelect={(_e, k) => setActiveTab(k as number)}>
                <Tab eventKey={0} title={<TabTitleText>System Prompt</TabTitleText>}>
                    <TabContent id="system-prompt-tab" eventKey={0} activeKey={activeTab}
                        style={{ marginTop: "16px" }}>
                        <p style={{ color: "#6a6e73", marginBottom: "16px" }}>
                            The system prompt defines the Manager's role, behavior, and decision
                            format. It is sent as the system context for every Manager evaluation.
                        </p>
                        <CodeEditor
                            code={config.systemPrompt || ""}
                            onCodeChange={(v) => { setConfig({ ...config, systemPrompt: v }); setDirty(true); }}
                            language={Language.markdown}
                            height="500px"
                            isLineNumbersVisible
                        />
                    </TabContent>
                </Tab>
                <Tab eventKey={1} title={<TabTitleText>Prompt Template</TabTitleText>}>
                    <TabContent id="prompt-template-tab" eventKey={1} activeKey={activeTab}
                        style={{ marginTop: "16px" }}>
                        <p style={{ color: "#6a6e73", marginBottom: "16px" }}>
                            The prompt template is sent as the user message for each event evaluation.
                            Placeholders are substituted at runtime:{" "}
                            <code>{"{{actionTypes}}"}</code> (list of configured action types),{" "}
                            <code>{"{{actors}}"}</code> (list of configured actors),{" "}
                            <code>{"{{source}}"}</code>,{" "}
                            <code>{"{{eventType}}"}</code>,{" "}
                            <code>{"{{issueRef}}"}</code>,{" "}
                            <code>{"{{repository}}"}</code>,{" "}
                            <code>{"{{payload}}"}</code> (raw event JSON),{" "}
                            <code>{"{{projectContext}}"}</code> (existing project and recent tasks).
                        </p>
                        <CodeEditor
                            code={config.promptTemplate || ""}
                            onCodeChange={(v) => { setConfig({ ...config, promptTemplate: v }); setDirty(true); }}
                            language={Language.markdown}
                            height="500px"
                            isLineNumbersVisible
                        />
                    </TabContent>
                </Tab>
                <Tab eventKey={2} title={<TabTitleText>Event History ({eventsTotalCount})</TabTitleText>}>
                    <TabContent id="events-tab" eventKey={2} activeKey={activeTab}
                        style={{ marginTop: "16px" }}>
                        <Toolbar>
                            <ToolbarContent>
                                <ToolbarItem>
                                    <Button variant="plain" aria-label="Refresh" onClick={loadEvents}>
                                        <SyncAltIcon />
                                    </Button>
                                </ToolbarItem>
                                <ToolbarItem variant="pagination" align={{ default: "alignEnd" }}>
                                    <Pagination
                                        itemCount={eventsTotalCount}
                                        page={eventsPage}
                                        perPage={eventsPerPage}
                                        onSetPage={(_e, p) => setEventsPage(p)}
                                        onPerPageSelect={(_e, pp) => { setEventsPerPage(pp); setEventsPage(1); }}
                                        isCompact
                                    />
                                </ToolbarItem>
                            </ToolbarContent>
                        </Toolbar>

                        {eventsLoading ? (
                            <EmptyState>
                                <EmptyStateBody>Loading event history...</EmptyStateBody>
                            </EmptyState>
                        ) : events.length === 0 ? (
                            <EmptyState>
                                <EmptyStateBody>
                                    No manager evaluations yet. Events will appear here
                                    as the Manager processes incoming events.
                                </EmptyStateBody>
                            </EmptyState>
                        ) : (
                            <Table aria-label="Manager Event History" variant="compact">
                                <Thead>
                                    <Tr>
                                        <Th>Time</Th>
                                        <Th>Event</Th>
                                        <Th>Type</Th>
                                        <Th>Summary</Th>
                                        <Th />
                                    </Tr>
                                </Thead>
                                <Tbody>
                                    {events.map((entry) => (
                                        <Tr key={entry.id}>
                                            <Td style={{ whiteSpace: "nowrap" }}>
                                                {new Date(entry.createdOn).toLocaleString()}
                                            </Td>
                                            <Td>
                                                {entry.eventId ? (
                                                    <Label isCompact color="blue">
                                                        #{entry.eventId}
                                                    </Label>
                                                ) : "—"}
                                            </Td>
                                            <Td>
                                                <Label isCompact
                                                    color={ENTRY_TYPE_COLORS[entry.entryType] || "grey"}>
                                                    {entry.entryType}
                                                </Label>
                                            </Td>
                                            <Td>
                                                {entry.summary && entry.summary.length > 120
                                                    ? entry.summary.substring(0, 117) + "..."
                                                    : entry.summary}
                                            </Td>
                                            <Td>
                                                {(entry.entryType === "manager-evaluated"
                                                        || entry.entryType === "manager-error") && (
                                                    <Button variant="link" isInline
                                                        onClick={() => handleViewLog(entry.id)}>
                                                        View Log
                                                    </Button>
                                                )}
                                            </Td>
                                        </Tr>
                                    ))}
                                </Tbody>
                            </Table>
                        )}
                    </TabContent>
                </Tab>
            </Tabs>

            <ExecutionLogModal
                isOpen={isLogModalOpen}
                activityId={logActivityId}
                onClose={() => setIsLogModalOpen(false)}
            />
        </PageSection>
    );
}
