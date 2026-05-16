package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CommandPreferenceVo(
        String commandId,
        boolean pinned,
        int usageCount,
        LocalDateTime lastUsedAt,
        LocalDateTime pinnedAt
) {
}
