package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.api.MetricsResource;
import io.apicurio.axiom.api.beans.MetricsSummary;
import io.apicurio.axiom.api.beans.ProjectMetricsSummary;
import io.apicurio.axiom.core.entities.ProjectEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the global Metrics REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class MetricsResourceImpl implements MetricsResource {

    @Inject
    EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public MetricsSummary getMetricsSummary() {
        // Aggregate disk usage from projects
        Object diskResult = entityManager.createQuery(
                "SELECT COALESCE(SUM(diskUsageBytes), 0) FROM ProjectEntity")
                .getSingleResult();
        long totalDiskUsage = ((Number) diskResult).longValue();

        // Aggregate AI usage
        Object[] aiResult = (Object[]) entityManager.createQuery(
                "SELECT COUNT(id), COALESCE(SUM(costUsd), 0), "
                        + "COALESCE(SUM(inputTokens), 0), COALESCE(SUM(outputTokens), 0) "
                        + "FROM AiUsageEntity")
                .getSingleResult();

        // Per-project breakdown
        List<ProjectEntity> projects = ProjectEntity.listAll();
        List<ProjectMetricsSummary> projectSummaries = new ArrayList<>();

        for (ProjectEntity project : projects) {
            Object[] projectAi = (Object[]) entityManager.createQuery(
                    "SELECT COUNT(id), COALESCE(SUM(costUsd), 0) "
                            + "FROM AiUsageEntity WHERE projectId = :pid")
                    .setParameter("pid", project.id)
                    .getSingleResult();

            ProjectMetricsSummary ps = new ProjectMetricsSummary();
            ps.setProjectId(project.id);
            ps.setProjectName(project.name);
            ps.setDiskUsageBytes(project.diskUsageBytes != null ? project.diskUsageBytes : 0L);
            ps.setCostUsd(((Number) projectAi[1]).doubleValue());
            ps.setInvocationCount(((Number) projectAi[0]).longValue());
            projectSummaries.add(ps);
        }

        MetricsSummary summary = new MetricsSummary();
        summary.setTotalDiskUsageBytes(totalDiskUsage);
        summary.setTotalCostUsd(((Number) aiResult[1]).doubleValue());
        summary.setTotalInputTokens(((Number) aiResult[2]).longValue());
        summary.setTotalOutputTokens(((Number) aiResult[3]).longValue());
        summary.setTotalInvocations(((Number) aiResult[0]).longValue());
        summary.setProjectCount(projects.size());
        summary.setProjects(projectSummaries);
        return summary;
    }
}
