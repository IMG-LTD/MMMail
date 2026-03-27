package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SearchPresetVo(
        String id,
        String name,
        String keyword,
        String folder,
        Boolean unread,
        Boolean starred,
        LocalDateTime from,
        LocalDateTime to,
        String label,
        boolean isPinned,
        LocalDateTime pinnedAt,
        int usageCount,
        LocalDateTime lastUsedAt
) {
}
