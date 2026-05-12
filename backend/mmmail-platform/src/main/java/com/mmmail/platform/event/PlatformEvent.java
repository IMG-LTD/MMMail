package com.mmmail.platform.event;

public record PlatformEvent(
        PlatformEventType type,
        String aggregateType,
        String aggregateId,
        PlatformEventMetadata metadata,
        String payloadJson,
        String idempotencyKey
) {

    public PlatformEvent {
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
        metadata.validateFor(type);
    }

    public String eventName() {
        return type.eventName();
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
