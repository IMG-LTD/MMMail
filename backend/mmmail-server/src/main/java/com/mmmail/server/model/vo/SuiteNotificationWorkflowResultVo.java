package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteNotificationWorkflowResultVo(
        LocalDateTime executedAt,
        String operation,
        int requestedCount,
        int affectedCount,
        String operationId,
        long syncCursor,
        String syncVersion
) {
}
