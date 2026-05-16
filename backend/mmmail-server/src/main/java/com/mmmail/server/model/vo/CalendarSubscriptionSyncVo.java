package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarSubscriptionSyncVo(
        String jobId,
        String subscriptionId,
        String syncStatus,
        int totalCount,
        int importedCount,
        List<String> eventIds,
        LocalDateTime syncedAt
) {
}
