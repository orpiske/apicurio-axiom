package io.apicurio.axiom.app.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.axiom.api.ToolsResource;
import io.apicurio.axiom.api.beans.NewToolDefinition;
import io.apicurio.axiom.api.beans.ToolAiEditRequest;
import io.apicurio.axiom.api.beans.ToolAiEditResponse;
import io.apicurio.axiom.api.beans.ToolDefinition;
import io.apicurio.axiom.api.beans.ToolParameter;
import io.apicurio.axiom.api.beans.ToolSearchResults;
import io.apicurio.axiom.app.ToolAiService;
import io.apicurio.axiom.core.entities.ToolDefinitionEntity;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Tools REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class ToolsResourceImpl implements ToolsResource {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ToolAiService toolAiService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolSearchResults listTools(BigInteger page, BigInteger limit, String filterName) {
        int pageNum = page != null ? page.intValue() : 1;
        int pageSize = limit != null ? limit.intValue() : 20;

        StringBuilder hql = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (filterName != null && !filterName.isBlank()) {
            hql.append(" and (lower(name) like :name or lower(description) like :name)");
            params.put("name", "%" + filterName.toLowerCase() + "%");
        }

        long totalCount = ToolDefinitionEntity.count(hql.toString(), params);
        List<ToolDefinition> items = ToolDefinitionEntity.<ToolDefinitionEntity>find(
                        hql.toString(), Sort.ascending("name"), params)
                .page(Page.of(pageNum - 1, pageSize))
                .list().stream().map(this::toBean).toList();

        ToolSearchResults results = new ToolSearchResults();
        results.setItems(items);
        results.setTotalCount(totalCount);
        results.setPage(pageNum);
        results.setLimit(pageSize);
        return results;
    }

    @Override
    @Transactional
    public ToolDefinition createTool(NewToolDefinition data) {
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        applyFields(entity, data);
        entity.persist();
        return toBean(entity);
    }

    @Override
    public ToolDefinition getTool(BigInteger toolId) {
        return toBean(findOrThrow(toolId.longValue()));
    }

    @Override
    @Transactional
    public ToolDefinition updateTool(BigInteger toolId, NewToolDefinition data) {
        ToolDefinitionEntity entity = findOrThrow(toolId.longValue());
        applyFields(entity, data);
        return toBean(entity);
    }

    @Override
    @Transactional
    public void deleteTool(BigInteger toolId) {
        ToolDefinitionEntity entity = findOrThrow(toolId.longValue());
        entity.delete();
    }

    // ── AI-Assisted Editing ────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolAiEditResponse aiEditTool(ToolAiEditRequest data) {
        return toolAiService.editTool(data);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private ToolDefinitionEntity findOrThrow(long id) {
        ToolDefinitionEntity entity = ToolDefinitionEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Tool not found: " + id, 404);
        }
        return entity;
    }

    private void applyFields(ToolDefinitionEntity entity, NewToolDefinition data) {
        entity.name = data.getName();
        entity.description = data.getDescription();
        entity.scriptTemplate = data.getScriptTemplate();

        if (data.getParameters() != null) {
            try {
                entity.parameters = objectMapper.writeValueAsString(data.getParameters());
            } catch (Exception e) {
                entity.parameters = null;
            }
        }
    }

    private ToolDefinition toBean(ToolDefinitionEntity entity) {
        ToolDefinition tool = new ToolDefinition();
        tool.setId(entity.id);
        tool.setName(entity.name);
        tool.setDescription(entity.description);
        tool.setScriptTemplate(entity.scriptTemplate);

        if (entity.parameters != null) {
            try {
                List<ToolParameter> params = objectMapper.readValue(entity.parameters,
                        new TypeReference<List<ToolParameter>>() {});
                tool.setParameters(params);
            } catch (Exception e) {
                // ignore
            }
        }

        return tool;
    }
}
