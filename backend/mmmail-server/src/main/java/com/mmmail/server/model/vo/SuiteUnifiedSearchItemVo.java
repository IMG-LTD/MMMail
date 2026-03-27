package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SuiteUnifiedSearchItemVo(
        String productCode,
        String itemType,
        String entityId,
        String title,
        String summary,
        String routePath,
        LocalDateTime updatedAt
) {
}
