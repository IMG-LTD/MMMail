package com.mmmail.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.platform.event.PlatformEvent;
import com.mmmail.platform.event.PlatformEventMetadata;
import com.mmmail.platform.event.PlatformEventType;
import com.mmmail.platform.outbox.OutboxPublishRequest;
import com.mmmail.platform.outbox.OutboxPublisher;
import com.mmmail.server.model.entity.V21CollaborationComment;
import com.mmmail.server.model.entity.V21CollaborationProject;
import com.mmmail.server.model.entity.V21CollaborationTask;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class V21CollaborationEventService {

    private static final String MODULE_COLLABORATION = "collaboration";
    private static final String TENANT_COMMUNITY = "community";

    private final AuditService auditService;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    public V21CollaborationEventService(
            AuditService auditService,
            OutboxPublisher outboxPublisher,
            ObjectMapper objectMapper
    ) {
        this.auditService = auditService;
        this.outboxPublisher = outboxPublisher;
        this.objectMapper = objectMapper;
    }

    public void recordProjectCreated(
            Long userId,
            V21CollaborationProject project,
            String ipAddress,
            LocalDateTime now
    ) {
        auditService.record(userId, "V21_COLLABORATION_PROJECT_CREATE", "projectId=" + project.getId(), ipAddress);
        publishEvent(userId, new EventEnvelope(
                PlatformEventType.COLLABORATION_PROJECT_CREATED,
                "collaboration_project",
                String.valueOf(project.getId()),
                "project.create",
                payload("projectId", project.getId(), "product", project.getProduct(), "status", project.getStatus()),
                "v21-collaboration-project-created-" + project.getId(),
                now
        ));
    }

    public void recordTaskCreated(
            Long userId,
            V21CollaborationTask task,
            String ipAddress,
            LocalDateTime now
    ) {
        auditService.record(userId, "V21_COLLABORATION_TASK_CREATE", "taskId=" + task.getId(), ipAddress);
        publishEvent(userId, new EventEnvelope(
                PlatformEventType.COLLABORATION_TASK_CREATED,
                "collaboration_task",
                String.valueOf(task.getId()),
                "task.create",
                taskPayload(task),
                "v21-collaboration-task-created-" + task.getId(),
                now
        ));
    }

    public void recordTaskUpdated(
            Long userId,
            V21CollaborationTask task,
            String ipAddress,
            LocalDateTime now
    ) {
        auditService.record(userId, "V21_COLLABORATION_TASK_UPDATE", "taskId=" + task.getId(), ipAddress);
        publishEvent(userId, new EventEnvelope(
                PlatformEventType.COLLABORATION_TASK_UPDATED_V1,
                "collaboration_task",
                String.valueOf(task.getId()),
                "task.update",
                taskPayload(task),
                "v21-collaboration-task-updated-" + task.getId() + "-" + now,
                now
        ));
    }

    public void recordCommentCreated(
            Long userId,
            V21CollaborationTask task,
            V21CollaborationComment comment,
            String ipAddress,
            LocalDateTime now
    ) {
        auditService.record(userId, "V21_COLLABORATION_COMMENT_CREATE", "commentId=" + comment.getId(), ipAddress);
        publishEvent(userId, new EventEnvelope(
                PlatformEventType.COLLABORATION_COMMENT_CREATED,
                "collaboration_comment",
                String.valueOf(comment.getId()),
                "comment.create",
                commentPayload(task, comment),
                "v21-collaboration-comment-created-" + comment.getId(),
                now
        ));
    }

    private void publishEvent(Long userId, EventEnvelope envelope) {
        PlatformEvent event = new PlatformEvent(
                envelope.type(),
                envelope.aggregateType(),
                envelope.aggregateId(),
                metadata(userId, envelope.operation(), envelope.occurredAt()),
                envelope.payloadJson(),
                envelope.idempotencyKey()
        );
        outboxPublisher.publish(new OutboxPublishRequest(event));
    }

    private PlatformEventMetadata metadata(Long userId, String operation, LocalDateTime now) {
        return new PlatformEventMetadata(
                TENANT_COMMUNITY,
                String.valueOf(userId),
                null,
                null,
                MODULE_COLLABORATION,
                operation,
                now
        );
    }

    private String taskPayload(V21CollaborationTask task) {
        return payload(
                "taskId", task.getId(),
                "projectId", task.getProjectId(),
                "ownerId", task.getOwnerId(),
                "product", task.getProduct(),
                "status", task.getStatus(),
                "boardColumn", task.getBoardColumn(),
                "position", task.getPosition()
        );
    }

    private String commentPayload(V21CollaborationTask task, V21CollaborationComment comment) {
        return payload(
                "commentId", comment.getId(),
                "taskId", task.getId(),
                "projectId", task.getProjectId(),
                "ownerId", task.getOwnerId(),
                "product", task.getProduct()
        );
    }

    private String payload(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize collaboration outbox payload", exception);
        }
    }

    private record EventEnvelope(
            PlatformEventType type,
            String aggregateType,
            String aggregateId,
            String operation,
            String payloadJson,
            String idempotencyKey,
            LocalDateTime occurredAt
    ) {
    }
}
