package com.mmmail.server.outbox;

import com.mmmail.platform.outbox.OutboxEventRecord;

@FunctionalInterface
public interface PlatformOutboxHandler {

    void handle(OutboxEventRecord eventRecord);
}
