package com.mmmail.platform.outbox;

public record OutboxPublishResult(Long eventId, OutboxEventStatus status, boolean duplicate) {
}
