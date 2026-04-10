import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
    Alert,
    Button,
    Card,
    CardBody,
    CardTitle,
    EmptyState,
    EmptyStateBody,
    Flex,
    FlexItem,
    Label,
    PageSection,
    Title,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import CheckCircleIcon from "@patternfly/react-icons/dist/esm/icons/check-circle-icon";
import ExclamationCircleIcon from "@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon";
import {
    type Project,
    type ActivityLogEntry,
    fetchProjects,
    fetchActivityLog,
    fetchActionTypes,
    fetchActors,
    fetchPolicies,
    fetchRepositories,
} from "../config/api";

const STATUS_COLORS: Record<string, "blue" | "green" | "orange" | "grey"> = {
    Created: "blue",
    InProgress: "green",
    Idle: "orange",
    Completed: "grey",
};

const STATUS_LABELS: Record<string, string> = {
    Created: "Created",
    InProgress: "In Progress",
    Idle: "Idle",
    Completed: "Completed",
};

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
};

interface SetupRequirement {
    name: string;
    met: boolean;
    description: string;
    navPath: string;
    navLabel: string;
}

export function DashboardPage() {
    const navigate = useNavigate();
    const [projects, setProjects] = useState<Project[]>([]);
    const [recentActivity, setRecentActivity] = useState<ActivityLogEntry[]>([]);
    const [loading, setLoading] = useState(true);
    const [requirements, setRequirements] = useState<SetupRequirement[]>([]);
    const [setupChecked, setSetupChecked] = useState(false);

    const loadData = useCallback(() => {
        setLoading(true);
        Promise.all([
            fetchProjects(),
            fetchActivityLog(),
            fetchRepositories(),
            fetchActors(),
            fetchPolicies(),
            fetchActionTypes(),
        ])
            .then(([p, a, repos, actors, policies, actionTypes]) => {
                setProjects(p);
                setRecentActivity(a.reverse().slice(0, 10));

                setRequirements([
                    {
                        name: "Repository",
                        met: repos.length > 0,
                        description:
                            "At least one GitHub or Jira repository must be configured for " +
                            "the system to monitor for events.",
                        navPath: "/repositories",
                        navLabel: "Configure Repositories",
                    },
                    {
                        name: "Actor",
                        met: actors.length > 0,
                        description:
                            "At least one actor (AI agent or human) must be configured to " +
                            "perform tasks assigned by the Manager.",
                        navPath: "/actors",
                        navLabel: "Configure Actors",
                    },
                    {
                        name: "Policy",
                        met: policies.length > 0,
                        description:
                            "At least one policy must be configured to guide the Manager's " +
                            "decision-making when evaluating events.",
                        navPath: "/policies",
                        navLabel: "Configure Policies",
                    },
                    {
                        name: "Action Type",
                        met: actionTypes.length > 0,
                        description:
                            "At least one action type must be registered to define the kinds " +
                            "of work that can be performed.",
                        navPath: "/action-types",
                        navLabel: "Configure Action Types",
                    },
                ]);
                setSetupChecked(true);
            })
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const unmetRequirements = requirements.filter((r) => !r.met);

    const statusCounts = projects.reduce(
        (acc, p) => {
            acc[p.status] = (acc[p.status] || 0) + 1;
            return acc;
        },
        {} as Record<string, number>
    );

    const activeProjects = projects.filter(
        (p) => p.status !== "Completed"
    );

    const setupIncomplete = setupChecked && unmetRequirements.length > 0;

    if (setupIncomplete) {
        return (
            <PageSection>
                <Title headingLevel="h1" size="lg">
                    Dashboard
                </Title>
                <Alert
                    variant="warning"
                    title="Setup Incomplete"
                    isInline
                    style={{ marginTop: "16px" }}
                >
                    <p style={{ marginBottom: "12px" }}>
                        The following configuration is required before the system can
                        process events and perform work:
                    </p>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <tbody>
                            {requirements.map((req) => (
                                <tr key={req.name} style={{ borderBottom: "1px solid #d2d2d2" }}>
                                    <td style={{ padding: "8px", width: "24px" }}>
                                        {req.met ? (
                                            <CheckCircleIcon color="var(--pf-t--global--color--status--success--default)" />
                                        ) : (
                                            <ExclamationCircleIcon color="var(--pf-t--global--color--status--warning--default)" />
                                        )}
                                    </td>
                                    <td style={{ padding: "8px", fontWeight: "bold", width: "120px" }}>
                                        {req.name}
                                    </td>
                                    <td style={{ padding: "8px" }}>
                                        {req.met ? (
                                            <span style={{ color: "var(--pf-t--global--color--status--success--default)" }}>
                                                Configured
                                            </span>
                                        ) : (
                                            <>
                                                {req.description}{" "}
                                                <Button
                                                    variant="link"
                                                    isInline
                                                    onClick={() => navigate(req.navPath)}
                                                >
                                                    {req.navLabel}
                                                </Button>
                                            </>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </Alert>
            </PageSection>
        );
    }

    return (
        <PageSection>
            <Title headingLevel="h1" size="lg">
                Dashboard
            </Title>

            {/* Status summary cards */}
            <Flex style={{ marginTop: "16px", gap: "16px" }}>
                {["Created", "InProgress", "Idle", "Completed"].map(
                    (status) => (
                        <FlexItem key={status}>
                            <Card isCompact>
                                <CardTitle>
                                    {STATUS_LABELS[status] || status}
                                </CardTitle>
                                <CardBody>
                                    <span style={{ fontSize: "24px", fontWeight: "bold" }}>
                                        {statusCounts[status] || 0}
                                    </span>
                                </CardBody>
                            </Card>
                        </FlexItem>
                    )
                )}
                <FlexItem>
                    <Card isCompact>
                        <CardTitle>Total</CardTitle>
                        <CardBody>
                            <span style={{ fontSize: "24px", fontWeight: "bold" }}>
                                {projects.length}
                            </span>
                        </CardBody>
                    </Card>
                </FlexItem>
            </Flex>

            {/* Active projects */}
            <Title headingLevel="h2" size="md" style={{ marginTop: "32px" }}>
                Active Projects
            </Title>
            <div style={{ marginTop: "8px" }}>
                {loading ? (
                    <EmptyState>
                        <EmptyStateBody>Loading...</EmptyStateBody>
                    </EmptyState>
                ) : activeProjects.length === 0 ? (
                    <EmptyState>
                        <EmptyStateBody>No active projects.</EmptyStateBody>
                    </EmptyState>
                ) : (
                    <Table aria-label="Active Projects" variant="compact">
                        <Thead>
                            <Tr>
                                <Th>Name</Th>
                                <Th>Status</Th>
                                <Th>Type</Th>
                                <Th>Issue</Th>
                                <Th>Updated</Th>
                            </Tr>
                        </Thead>
                        <Tbody>
                            {activeProjects.map((project) => (
                                <Tr
                                    key={project.id}
                                    isClickable
                                    onRowClick={() =>
                                        navigate(`/projects/${project.id}`)
                                    }
                                >
                                    <Td>{project.name}</Td>
                                    <Td>
                                        <Label color={STATUS_COLORS[project.status] || "grey"}>
                                            {STATUS_LABELS[project.status] || project.status}
                                        </Label>
                                    </Td>
                                    <Td>
                                        <Label isCompact>{project.type}</Label>
                                    </Td>
                                    <Td>{project.issueRef}</Td>
                                    <Td>{new Date(project.updatedOn).toLocaleString()}</Td>
                                </Tr>
                            ))}
                        </Tbody>
                    </Table>
                )}
            </div>

            {/* Recent activity */}
            <Title headingLevel="h2" size="md" style={{ marginTop: "32px" }}>
                Recent Activity
            </Title>
            <div style={{ marginTop: "8px" }}>
                {recentActivity.length === 0 ? (
                    <EmptyState>
                        <EmptyStateBody>No recent activity.</EmptyStateBody>
                    </EmptyState>
                ) : (
                    <Table aria-label="Recent Activity" variant="compact">
                        <Thead>
                            <Tr>
                                <Th>Time</Th>
                                <Th>Type</Th>
                                <Th>Summary</Th>
                            </Tr>
                        </Thead>
                        <Tbody>
                            {recentActivity.map((entry) => (
                                <Tr key={entry.id}>
                                    <Td style={{ whiteSpace: "nowrap" }}>
                                        {new Date(entry.createdOn).toLocaleString()}
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
                                </Tr>
                            ))}
                        </Tbody>
                    </Table>
                )}
            </div>
        </PageSection>
    );
}
