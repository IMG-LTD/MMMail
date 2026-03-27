package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteNotificationMarkReadResultVo(
        LocalDateTime executedAt,
        int requestedCount,
        int affectedCount,
        long syncCursor,
        String syncVersion
) {
}
