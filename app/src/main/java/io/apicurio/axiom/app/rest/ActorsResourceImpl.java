package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.api.ActorsResource;
import io.apicurio.axiom.api.beans.Actor;
import io.apicurio.axiom.api.beans.NewActor;
import io.apicurio.axiom.core.entities.ActorEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.util.List;

/**
 * Implementation of the Actors REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class ActorsResourceImpl implements ActorsResource {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Actor> listActors() {
        return ActorEntity.<ActorEntity>listAll()
                .stream()
                .map(this::toBean)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Actor createActor(NewActor data) {
        ActorEntity entity = new ActorEntity();
        applyFields(entity, data);
        entity.persist();
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Actor getActor(long actorId) {
        return toBean(findOrThrow(actorId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Actor updateActor(long actorId, NewActor data) {
        ActorEntity entity = findOrThrow(actorId);
        applyFields(entity, data);
        return toBean(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteActor(long actorId) {
        ActorEntity entity = findOrThrow(actorId);
        entity.delete();
    }

    private void applyFields(ActorEntity entity, NewActor body) {
        entity.name = body.getName();
        entity.description = body.getDescription();
        entity.type = body.getType().value();
        entity.capabilities = body.getCapabilities() != null ? String.join(",", body.getCapabilities()) : null;
        entity.permissions = body.getPermissions() != null ? body.getPermissions().toString() : null;
        entity.configuration = body.getConfiguration() != null ? body.getConfiguration().toString() : null;
    }

    private ActorEntity findOrThrow(long id) {
        ActorEntity entity = ActorEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Actor not found: " + id, 404);
        }
        return entity;
    }

    private Actor toBean(ActorEntity entity) {
        Actor actor = new Actor();
        actor.setId(entity.id);
        actor.setName(entity.name);
        actor.setDescription(entity.description);
        actor.setType(Actor.Type.fromValue(entity.type));
        if (entity.capabilities != null && !entity.capabilities.isEmpty()) {
            actor.setCapabilities(List.of(entity.capabilities.split(",")));
        }
        return actor;
    }
}
