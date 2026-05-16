package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CommandRecentVo(
        String commandId,
        String title,
        String group,
        String routePath,
        LocalDateTime lastUsedAt,
        int usageCount
) {
}
