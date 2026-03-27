package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record LumoProjectKnowledgeVo(
        String knowledgeId,
        String projectId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
