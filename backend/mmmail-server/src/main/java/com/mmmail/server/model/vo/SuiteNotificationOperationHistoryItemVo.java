package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteNotificationOperationHistoryItemVo(
        String operationId,
        String operation,
        int requestedCount,
        int affectedCount,
        LocalDateTime executedAt,
        boolean undoAvailable
) {
}
