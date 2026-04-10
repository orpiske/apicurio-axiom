import { useState, useEffect, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
    Button,
    EmptyState,
    EmptyStateBody,
    Flex,
    FlexItem,
    Label,
    PageSection,
    Select,
    SelectOption,
    MenuToggle,
    MenuToggleElement,
    TextInput,
    Title,
    Toolbar,
    ToolbarContent,
    ToolbarItem,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import SyncAltIcon from "@patternfly/react-icons/dist/esm/icons/sync-alt-icon";
import FilterIcon from "@patternfly/react-icons/dist/esm/icons/filter-icon";
import TimesIcon from "@patternfly/react-icons/dist/esm/icons/times-icon";
import { type ActivityLogEntry, fetchActivityLog } from "../config/api";

const ENTRY_TYPE_COLORS: Record<string, "blue" | "green" | "orange" | "grey" | "red"> = {
    "event-received": "blue",
    "task-created": "green",
    "task-started": "green",
    "task-completed": "green",
    "task-failed": "red",
    "project-created": "blue",
    "project-closed": "grey",
    "project-reopened": "orange",
    "event-ignored": "grey",
    "manager-escalation": "orange",
    "manager-no-decision": "grey",
    "manager-skipped": "grey",
    "manager-error": "red",
    "pipeline-error": "red",
    "task-awaiting-input": "orange",
};

const ALL_ENTRY_TYPES = Object.keys(ENTRY_TYPE_COLORS);

export function ActivityLogPage() {
    const navigate = useNavigate();
    const [entries, setEntries] = useState<ActivityLogEntry[]>([]);
    const [loading, setLoading] = useState(true);

    // Filter state
    const [filterEventId, setFilterEventId] = useState("");
    const [filterSummary, setFilterSummary] = useState("");
    const [filterProjectId, setFilterProjectId] = useState("");
    const [filterEntryTypes, setFilterEntryTypes] = useState<string[]>([]);
    const [isTypeSelectOpen, setIsTypeSelectOpen] = useState(false);

    const loadData = useCallback(() => {
        setLoading(true);
        fetchActivityLog()
            .then((data) => setEntries(data.reverse()))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const hasActiveFilters = filterEventId || filterSummary || filterProjectId || filterEntryTypes.length > 0;

    const clearFilters = () => {
        setFilterEventId("");
        setFilterSummary("");
        setFilterProjectId("");
        setFilterEntryTypes([]);
    };

    const filteredEntries = useMemo(() => {
        return entries.filter((entry) => {
            if (filterEventId && (!entry.eventId || !entry.eventId.toString().includes(filterEventId))) {
                return false;
            }
            if (filterProjectId && (!entry.projectId || !entry.projectId.toString().includes(filterProjectId))) {
                return false;
            }
            if (filterSummary && !entry.summary.toLowerCase().includes(filterSummary.toLowerCase())) {
                return false;
            }
            if (filterEntryTypes.length > 0 && !filterEntryTypes.includes(entry.entryType)) {
                return false;
            }
            return true;
        });
    }, [entries, filterEventId, filterSummary, filterProjectId, filterEntryTypes]);

    const onTypeSelect = (_event: React.MouseEvent | undefined, value: string | number | undefined) => {
        const val = value as string;
        setFilterEntryTypes((prev) =>
            prev.includes(val) ? prev.filter((t) => t !== val) : [...prev, val]
        );
    };

    const typeToggle = (toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
            ref={toggleRef}
            onClick={() => setIsTypeSelectOpen(!isTypeSelectOpen)}
            isExpanded={isTypeSelectOpen}
            style={{ minWidth: "180px" }}
        >
            {filterEntryTypes.length > 0
                ? `${filterEntryTypes.length} type${filterEntryTypes.length > 1 ? "s" : ""} selected`
                : "Entry Type"}
        </MenuToggle>
    );

    return (
        <PageSection>
            <Flex
                justifyContent={{ default: "justifyContentSpaceBetween" }}
                alignItems={{ default: "alignItemsCenter" }}
            >
                <FlexItem>
                    <Title headingLevel="h1" size="lg">
                        Activity Log
                    </Title>
                </FlexItem>
                <FlexItem>
                    <Button variant="secondary" icon={<SyncAltIcon />} onClick={loadData}>
                        Refresh
                    </Button>
                </FlexItem>
            </Flex>

            {/* Filter toolbar */}
            <Toolbar clearAllFilters={clearFilters} style={{ marginTop: "8px" }}>
                <ToolbarContent>
                    <ToolbarItem>
                        <FilterIcon style={{ marginRight: "8px", color: "#6a6e73" }} />
                    </ToolbarItem>
                    <ToolbarItem>
                        <TextInput
                            type="text"
                            aria-label="Filter by event ID"
                            placeholder="Event ID"
                            value={filterEventId}
                            onChange={(_e, v) => setFilterEventId(v)}
                            style={{ width: "120px" }}
                        />
                    </ToolbarItem>
                    <ToolbarItem>
                        <Select
                            aria-label="Filter by entry type"
                            toggle={typeToggle}
                            onSelect={onTypeSelect}
                            selected={filterEntryTypes}
                            isOpen={isTypeSelectOpen}
                            onOpenChange={setIsTypeSelectOpen}
                        >
                            {ALL_ENTRY_TYPES.map((type) => (
                                <SelectOption
                                    key={type}
                                    value={type}
                                    hasCheckbox
                                    isSelected={filterEntryTypes.includes(type)}
                                >
                                    <Label isCompact color={ENTRY_TYPE_COLORS[type] || "grey"}>
                                        {type}
                                    </Label>
                                </SelectOption>
                            ))}
                        </Select>
                    </ToolbarItem>
                    <ToolbarItem>
                        <TextInput
                            type="text"
                            aria-label="Filter by summary"
                            placeholder="Summary search"
                            value={filterSummary}
                            onChange={(_e, v) => setFilterSummary(v)}
                            style={{ width: "200px" }}
                        />
                    </ToolbarItem>
                    <ToolbarItem>
                        <TextInput
                            type="text"
                            aria-label="Filter by project ID"
                            placeholder="Project ID"
                            value={filterProjectId}
                            onChange={(_e, v) => setFilterProjectId(v)}
                            style={{ width: "120px" }}
                        />
                    </ToolbarItem>
                    {hasActiveFilters && (
                        <ToolbarItem>
                            <Button variant="link" icon={<TimesIcon />} onClick={clearFilters}>
                                Clear filters
                            </Button>
                        </ToolbarItem>
                    )}
                    <ToolbarItem align={{ default: "alignEnd" }}>
                        <span style={{ color: "#6a6e73", fontSize: "14px" }}>
                            {filteredEntries.length} of {entries.length} entries
                        </span>
                    </ToolbarItem>
                </ToolbarContent>
            </Toolbar>

            <div style={{ marginTop: "8px" }}>
                {loading ? (
                    <EmptyState>
                        <EmptyStateBody>Loading activity log...</EmptyStateBody>
                    </EmptyState>
                ) : filteredEntries.length === 0 ? (
                    <EmptyState>
                        <EmptyStateBody>
                            {hasActiveFilters
                                ? "No entries match the current filters."
                                : "No activity yet."}
                        </EmptyStateBody>
                    </EmptyState>
                ) : (
                    <Table aria-label="Activity Log" variant="compact">
                        <Thead>
                            <Tr>
                                <Th>Time</Th>
                                <Th>Event</Th>
                                <Th>Type</Th>
                                <Th>Summary</Th>
                                <Th>Project</Th>
                            </Tr>
                        </Thead>
                        <Tbody>
                            {filteredEntries.map((entry) => (
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
                                        <Label
                                            isCompact
                                            color={ENTRY_TYPE_COLORS[entry.entryType] || "grey"}
                                        >
                                            {entry.entryType}
                                        </Label>
                                    </Td>
                                    <Td>{entry.summary}</Td>
                                    <Td>
                                        {entry.projectId ? (
                                            <Button
                                                variant="link"
                                                isInline
                                                onClick={() =>
                                                    navigate(`/projects/${entry.projectId}`)
                                                }
                                            >
                                                Project #{entry.projectId}
                                            </Button>
                                        ) : (
                                            "—"
                                        )}
                                    </Td>
                                </Tr>
                            ))}
                        </Tbody>
                    </Table>
                )}
            </div>
        </PageSection>
    );
}
