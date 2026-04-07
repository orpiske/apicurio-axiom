package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.api.ActivityResource;
import io.apicurio.axiom.api.beans.ActivityLogEntry;
import io.apicurio.axiom.core.entities.ActivityLogEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Date;
import java.util.List;

/**
 * Implementation of the Activity Log REST API.
 */
@ApplicationScoped
@RunOnVirtualThread
public class ActivityResourceImpl implements ActivityResource {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ActivityLogEntry> listActivityLog() {
        return ActivityLogEntity.<ActivityLogEntity>listAll()
                .stream()
                .map(this::toBean)
                .toList();
    }

    private ActivityLogEntry toBean(ActivityLogEntity entity) {
        ActivityLogEntry entry = new ActivityLogEntry();
        entry.setId(entity.id);
        entry.setProjectId(entity.projectId);
        entry.setTaskId(entity.taskId);
        entry.setEventId(entity.eventId);
        entry.setEntryType(entity.entryType);
        entry.setSummary(entity.summary);
        entry.setDetails(entity.details);
        entry.setCreatedOn(Date.from(entity.createdOn));
        return entry;
    }
}
