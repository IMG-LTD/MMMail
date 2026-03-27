package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteNotificationOperationHistoryVo(
        LocalDateTime generatedAt,
        int limit,
        int total,
        long syncCursor,
        String syncVersion,
        List<SuiteNotificationOperationHistoryItemVo> items
) {
}
