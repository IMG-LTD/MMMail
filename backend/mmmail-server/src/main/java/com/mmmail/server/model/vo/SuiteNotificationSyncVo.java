package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SuiteNotificationSyncVo(
        String kind,
        LocalDateTime generatedAt,
        long syncCursor,
        String syncVersion,
        boolean hasUpdates,
        int total,
        List<SuiteNotificationSyncEventVo> items
) {
}
