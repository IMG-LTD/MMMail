package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record LumoConversationVo(
        String conversationId,
        String projectId,
        String title,
        boolean pinned,
        String modelCode,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
