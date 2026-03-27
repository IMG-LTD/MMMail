package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record LumoProjectVo(
        String projectId,
        String name,
        String description,
        long conversationCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
