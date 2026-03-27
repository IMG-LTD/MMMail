package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MailFolderNodeVo(
        String id,
        String parentId,
        String name,
        String color,
        boolean notificationsEnabled,
        long unreadCount,
        long totalCount,
        LocalDateTime updatedAt,
        List<MailFolderNodeVo> children
) {
}
