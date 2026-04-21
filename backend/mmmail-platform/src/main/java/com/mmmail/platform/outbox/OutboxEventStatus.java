package com.mmmail.platform.outbox;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTER
}
