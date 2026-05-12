package com.mmmail.platform.outbox;

import java.time.LocalDateTime;

public interface OutboxDispatcher {

    OutboxDispatchResult dispatchDue(int limit, LocalDateTime now);
}
