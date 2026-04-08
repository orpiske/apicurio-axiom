package io.apicurio.axiom.app.rest;

import io.apicurio.axiom.core.entities.EventEntity;
import io.apicurio.axiom.manager.ManagerDecision;
import io.apicurio.axiom.manager.ManagerService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Debug endpoint for invoking the AI Manager manually against a specific event.
 */
@Path("/api/v1/manager")
@ApplicationScoped
@RunOnVirtualThread
public class ManagerResourceImpl {

    @Inject
    ManagerService managerService;

    /**
     * Evaluates an event with the AI Manager and returns its decisions.
     *
     * @param eventId the event to evaluate
     * @return the list of Manager decisions
     */
    @POST
    @Path("/evaluate/{eventId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public List<ManagerDecision> evaluateEvent(@PathParam("eventId") long eventId) {
        EventEntity event = EventEntity.findById(eventId);
        if (event == null) {
            throw new WebApplicationException("Event not found: " + eventId, 404);
        }
        return managerService.evaluate(event);
    }
}
