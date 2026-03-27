package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteNotificationCenterVo(
        LocalDateTime generatedAt,
        int limit,
        int total,
        int criticalCount,
        int unreadCount,
        long syncCursor,
        String syncVersion,
        List<SuiteNotificationItemVo> items
) {
}
