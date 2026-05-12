package com.mmmail.platform.outbox;

import com.mmmail.platform.event.PlatformEvent;
import com.mmmail.platform.event.PlatformEventMetadata;
import com.mmmail.platform.event.PlatformEventType;

import java.time.LocalDateTime;

public record OutboxEventRecord(
        Long id,
        PlatformEventType type,
        String ownerModule,
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        String idempotencyKey,
        OutboxEventStatus status,
        int attempts,
        LocalDateTime nextAttemptAt,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {

    public OutboxEventRecord {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        requireText(ownerModule, "ownerModule");
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        requireText(payloadJson, "payloadJson");
        requireText(idempotencyKey, "idempotencyKey");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts cannot be negative");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("createdAt and updatedAt are required");
        }
    }

    public static OutboxEventRecord pending(Long id, PlatformEvent event, LocalDateTime now) {
        PlatformEventMetadata metadata = event.metadata();
        return new OutboxEventRecord(
                id,
                event.type(),
                event.ownerModule(),
                metadata.tenantId(),
                metadata.userId(),
                metadata.requestId(),
                metadata.traceId(),
                event.aggregateType(),
                event.aggregateId(),
                event.payloadJson(),
                event.idempotencyKey(),
                OutboxEventStatus.PENDING,
                0,
                null,
                null,
                now,
                now,
                null
        );
    }

    public OutboxEventRecord markPublished(LocalDateTime now) {
        requireStatus(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED);
        return copy(OutboxEventStatus.PUBLISHED, attempts, null, null, now, now);
    }

    public OutboxEventRecord markFailed(String error, LocalDateTime nextAttemptAt) {
        requireStatus(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED);
        return copy(OutboxEventStatus.FAILED, attempts + 1, nextAttemptAt, trimError(error), LocalDateTime.now(), null);
    }

    public OutboxEventRecord markPendingForRetry(LocalDateTime now) {
        requireStatus(OutboxEventStatus.FAILED);
        return copy(OutboxEventStatus.PENDING, attempts, null, lastError, now, null);
    }

    public OutboxEventRecord markDeadLetter(String error, LocalDateTime now) {
        requireStatus(OutboxEventStatus.FAILED);
        return copy(OutboxEventStatus.DEAD_LETTER, attempts, null, trimError(error), now, null);
    }

    public String eventName() {
        return type.eventName();
    }

    private OutboxEventRecord copy(
            OutboxEventStatus newStatus,
            int newAttempts,
            LocalDateTime newNextAttemptAt,
            String newLastError,
            LocalDateTime newUpdatedAt,
            LocalDateTime newPublishedAt
    ) {
        return new OutboxEventRecord(
                id, type, ownerModule, tenantId, userId, requestId, traceId, aggregateType,
                aggregateId, payloadJson, idempotencyKey, newStatus, newAttempts, newNextAttemptAt,
                newLastError, createdAt, newUpdatedAt, newPublishedAt
        );
    }

    private void requireStatus(OutboxEventStatus... allowed) {
        for (OutboxEventStatus candidate : allowed) {
            if (status == candidate) {
                return;
            }
        }
        throw new IllegalStateException("Invalid outbox status transition from " + status);
    }

    private static String trimError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 512 ? error : error.substring(0, 512);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
