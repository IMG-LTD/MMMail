package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SearchHistoryVo(
        String id,
        String keyword,
        int usageCount,
        LocalDateTime lastUsedAt
) {
}
