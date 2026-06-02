package io.apitomy.axiom.actors.human;

import io.apitomy.axiom.actors.spi.Actor;
import io.apitomy.axiom.actors.spi.ActorContext;
import io.apitomy.axiom.actors.spi.TaskResult;
import io.apitomy.axiom.core.entities.TaskEntity;
import io.apitomy.axiom.core.events.SseEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Actor implementation for human-performed tasks. Instead of executing work
 * automatically, it notifies the user and waits for a response.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Task is assigned → notification sent to user</li>
 *   <li>Task status set to AwaitingInput</li>
 *   <li>User responds via the REST API</li>
 *   <li>Response completes the pending future</li>
 * </ol>
 */
@ApplicationScoped
public class HumanActor implements Actor {

    private static final Logger LOG = Logger.getLogger(HumanActor.class);

    private final Map<Long, CompletableFuture<TaskResult>> pendingTasks = new ConcurrentHashMap<>();

    @Inject
    Event<SseEvent> sseEvents;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "human";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<TaskResult> execute(TaskEntity task, ActorContext context) {
        LOG.infof("Human task %d (%s) awaiting user response", task.id, task.actionType);

        CompletableFuture<TaskResult> future = new CompletableFuture<>();
        pendingTasks.put(task.id, future);

        // Notify the user
        sseEvents.fire(SseEvent.notification(
                "Task assigned to you: " + task.actionType
                        + (task.input != null ? " — " + task.input : ""),
                "info"));

        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(TaskEntity task) {
        CompletableFuture<TaskResult> future = pendingTasks.remove(task.id);
        if (future != null) {
            LOG.infof("Cancelling human task %d", task.id);
            future.complete(TaskResult.failure("Task cancelled").build());
        }
    }

    /**
     * Submits a human response for a pending task.
     *
     * @param taskId the task ID
     * @param response the user's response text
     * @return true if the response was accepted (task was pending)
     */
    public boolean submitResponse(Long taskId, String response) {
        CompletableFuture<TaskResult> future = pendingTasks.remove(taskId);
        if (future == null) {
            LOG.warnf("No pending human task found for ID %d", taskId);
            return false;
        }

        LOG.infof("Human response received for task %d", taskId);
        future.complete(TaskResult.success(response).build());
        return true;
    }

    /**
     * Checks whether a task is currently awaiting a human response.
     *
     * @param taskId the task ID
     * @return true if the task is pending
     */
    public boolean isAwaitingResponse(Long taskId) {
        return pendingTasks.containsKey(taskId);
    }
}
