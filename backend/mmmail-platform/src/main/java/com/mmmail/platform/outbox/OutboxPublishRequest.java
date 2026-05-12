package com.mmmail.platform.outbox;

import com.mmmail.platform.event.PlatformEvent;

public record OutboxPublishRequest(PlatformEvent event) {

    public OutboxPublishRequest {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
    }
}
