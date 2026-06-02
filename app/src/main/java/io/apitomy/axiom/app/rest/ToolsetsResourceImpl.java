package io.apitomy.axiom.app.rest;

import io.apitomy.axiom.api.ToolsetsResource;
import io.apitomy.axiom.api.beans.NewToolset;
import io.apitomy.axiom.api.beans.Toolset;
import io.apitomy.axiom.core.entities.ToolsetEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the Toolsets REST API. Provides CRUD operations
 * for named collections of tools that can be referenced via the
 * {@code @ToolsetName} syntax in allowed tools configurations.
 */
@ApplicationScoped
@RunOnVirtualThread
public class ToolsetsResourceImpl implements ToolsetsResource {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Toolset> listToolsets() {
        return ToolsetEntity.<ToolsetEntity>listAll()
                .stream().map(this::toBean).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Toolset createToolset(NewToolset data) {
        ToolsetEntity entity = new ToolsetEntity();
        applyFields(entity, data);
        entity.persist();
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Toolset getToolset(long toolsetId) {
        return toBean(findOrThrow(toolsetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Toolset updateToolset(long toolsetId, NewToolset data) {
        ToolsetEntity entity = findOrThrow(toolsetId);
        applyFields(entity, data);
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteToolset(long toolsetId) {
        ToolsetEntity entity = findOrThrow(toolsetId);
        entity.delete();
    }

    private ToolsetEntity findOrThrow(long id) {
        ToolsetEntity entity = ToolsetEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Toolset not found: " + id, 404);
        }
        return entity;
    }

    private void applyFields(ToolsetEntity entity, NewToolset data) {
        entity.name = data.getName();
        entity.description = data.getDescription();
        if (data.getTools() != null) {
            entity.tools = String.join(", ", data.getTools());
        } else {
            entity.tools = "";
        }
    }

    private Toolset toBean(ToolsetEntity entity) {
        Toolset toolset = new Toolset();
        toolset.setId(entity.id);
        toolset.setName(entity.name);
        toolset.setDescription(entity.description);
        if (entity.tools != null && !entity.tools.isBlank()) {
            toolset.setTools(Arrays.stream(entity.tools.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        } else {
            toolset.setTools(List.of());
        }
        return toolset;
    }
}
