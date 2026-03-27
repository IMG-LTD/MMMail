package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteCommandFeedItemVo(
        Long eventId,
        String eventType,
        String category,
        String title,
        String detail,
        String productCode,
        String routePath,
        String ipAddress,
        LocalDateTime createdAt
) {
}
