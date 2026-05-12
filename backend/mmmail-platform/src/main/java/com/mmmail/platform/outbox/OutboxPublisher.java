package com.mmmail.platform.outbox;

public interface OutboxPublisher {

    OutboxPublishResult publish(OutboxPublishRequest request);
}
