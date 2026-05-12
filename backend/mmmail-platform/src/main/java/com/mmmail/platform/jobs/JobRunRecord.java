package com.mmmail.platform.jobs;

import java.time.LocalDateTime;

public record JobRunRecord(
        Long id,
        JobRunType type,
        String ownerModule,
        String tenantId,
        String userId,
        String requestId,
        String traceId,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        String idempotencyKey,
        JobRunState status,
        int progressPercent,
        int attempts,
        int maxAttempts,
        LocalDateTime nextAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String resultJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public JobRunRecord {
        validateRequired(type, ownerModule, aggregateType, aggregateId, payloadJson, idempotencyKey, status);
        if (!type.ownerModule().equals(ownerModule)) {
            throw new IllegalArgumentException("ownerModule must match job owner " + type.ownerModule());
        }
        if (type.tenantRequired() && !hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId is required for " + type.jobName());
        }
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }
        if (attempts < 0 || maxAttempts < 1 || attempts > maxAttempts) {
            throw new IllegalArgumentException("attempts must be between 0 and maxAttempts");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("createdAt and updatedAt are required");
        }
    }

    public static JobRunRecord queued(Long id, JobRunRequest request, LocalDateTime now) {
        JobRunMetadata metadata = request.metadata();
        return new JobRunRecord(
                id,
                request.type(),
                request.ownerModule(),
                metadata.tenantId(),
                metadata.userId(),
                metadata.requestId(),
                metadata.traceId(),
                request.aggregateType(),
                request.aggregateId(),
                request.payloadJson(),
                request.idempotencyKey(),
                JobRunState.QUEUED,
                0,
                0,
                request.maxAttempts(),
                null,
                null,
                null,
                null,
                now,
                now,
                null,
                null
        );
    }

    public String jobName() {
        return type.jobName();
    }

    public JobRunRecord markRunning(LocalDateTime now) {
        requireTransition(JobRunState.RUNNING, JobRunState.QUEUED);
        LocalDateTime started = startedAt == null ? now : startedAt;
        return copy(JobRunState.RUNNING, progressPercent, attempts, null, null, null, resultJson, now, started, null);
    }

    public JobRunRecord markWaitingApproval(LocalDateTime now) {
        requireTransition(JobRunState.WAITING_APPROVAL, JobRunState.RUNNING);
        return copy(JobRunState.WAITING_APPROVAL, progressPercent, attempts, null, null, null, resultJson, now, startedAt, null);
    }

    public JobRunRecord markSucceeded(String resultJson, LocalDateTime now) {
        requireTransition(JobRunState.SUCCEEDED, JobRunState.RUNNING);
        requireText(resultJson, "resultJson");
        return copy(JobRunState.SUCCEEDED, 100, attempts, null, null, null, resultJson, now, startedAt, now);
    }

    public JobRunRecord markFailed(String errorCode, String errorMessage, LocalDateTime now) {
        requireTransition(JobRunState.FAILED, JobRunState.RUNNING, JobRunState.WAITING_APPROVAL);
        requireText(errorCode, "errorCode");
        return copy(JobRunState.FAILED, progressPercent, attempts, null, errorCode, errorMessage, resultJson, now, startedAt, now);
    }

    public JobRunRecord markRetryable(
            String errorCode,
            String errorMessage,
            LocalDateTime nextAttemptAt,
            LocalDateTime now
    ) {
        requireTransition(JobRunState.RETRYABLE, JobRunState.RUNNING);
        if (!type.retrySupported() || attempts + 1 >= maxAttempts) {
            throw new IllegalStateException("job run has no remaining retry attempts");
        }
        requireText(errorCode, "errorCode");
        if (nextAttemptAt == null) {
            throw new IllegalArgumentException("nextAttemptAt is required");
        }
        return copy(JobRunState.RETRYABLE, progressPercent, attempts + 1, nextAttemptAt, errorCode, errorMessage, resultJson, now, startedAt, null);
    }

    public JobRunRecord markQueuedForRetry(LocalDateTime now) {
        requireTransition(JobRunState.QUEUED, JobRunState.RETRYABLE);
        return copy(JobRunState.QUEUED, progressPercent, attempts, null, lastErrorCode, lastErrorMessage, resultJson, now, startedAt, null);
    }

    public JobRunRecord withProgress(int progressPercent, LocalDateTime now) {
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }
        requireTransition(status, JobRunState.RUNNING, JobRunState.WAITING_APPROVAL);
        return copy(status, progressPercent, attempts, nextAttemptAt, lastErrorCode, lastErrorMessage, resultJson, now, startedAt, completedAt);
    }

    private JobRunRecord copy(
            JobRunState status,
            int progressPercent,
            int attempts,
            LocalDateTime nextAttemptAt,
            String lastErrorCode,
            String lastErrorMessage,
            String resultJson,
            LocalDateTime updatedAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        return new JobRunRecord(
                id,
                type,
                ownerModule,
                tenantId,
                userId,
                requestId,
                traceId,
                aggregateType,
                aggregateId,
                payloadJson,
                idempotencyKey,
                status,
                progressPercent,
                attempts,
                maxAttempts,
                nextAttemptAt,
                lastErrorCode,
                lastErrorMessage,
                resultJson,
                createdAt,
                updatedAt,
                startedAt,
                completedAt
        );
    }

    private void requireTransition(JobRunState target, JobRunState... allowedSources) {
        for (JobRunState allowed : allowedSources) {
            if (status == allowed) {
                return;
            }
        }
        throw new IllegalStateException("Invalid job run status transition from " + status + " to " + target);
    }

    private static void validateRequired(
            JobRunType type,
            String ownerModule,
            String aggregateType,
            String aggregateId,
            String payloadJson,
            String idempotencyKey,
            JobRunState status
    ) {
        if (type == null || status == null) {
            throw new IllegalArgumentException("type and status are required");
        }
        requireText(ownerModule, "ownerModule");
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        requireText(payloadJson, "payloadJson");
        requireText(idempotencyKey, "idempotencyKey");
    }

    private static void requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
