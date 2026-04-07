package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.api.EventsResource;
import io.apicurio.axiom.api.beans.Event;
import io.apicurio.axiom.core.entities.EventEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Date;
import java.util.List;

/**
 * Implementation of the Events REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class EventsResourceImpl implements EventsResource {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> listEvents() {
        return EventEntity.<EventEntity>listAll()
                .stream()
                .map(this::toBean)
                .toList();
    }

    private Event toBean(EventEntity entity) {
        Event event = new Event();
        event.setId(entity.id);
        event.setSource(entity.source);
        event.setEventType(entity.eventType);
        event.setIssueRef(entity.issueRef);
        event.setRepository(entity.repository);
        event.setProjectId(entity.projectId);
        event.setTaskId(entity.taskId);
        event.setPayload(entity.payload);
        event.setReceivedAt(Date.from(entity.receivedAt));
        return event;
    }
}
