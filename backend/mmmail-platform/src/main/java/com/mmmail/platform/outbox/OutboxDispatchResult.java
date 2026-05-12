package com.mmmail.platform.outbox;

public record OutboxDispatchResult(int scanned, int published, int failed, int deadLettered) {
}
