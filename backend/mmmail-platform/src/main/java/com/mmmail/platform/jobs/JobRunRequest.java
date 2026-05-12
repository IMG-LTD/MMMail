package com.mmmail.platform.jobs;

public record JobRunRequest(
        JobRunType type,
        JobRunMetadata metadata,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        String idempotencyKey,
        int maxAttempts
) {

    public JobRunRequest {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata is required");
        }
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        requireText(payloadJson, "payloadJson");
        requireText(idempotencyKey, "idempotencyKey");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        metadata.validateFor(type);
    }

    public String jobName() {
        return type.jobName();
    }

    public String ownerModule() {
        return type.ownerModule();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
