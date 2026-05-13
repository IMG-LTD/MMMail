package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationTaskVo(
        String id,
        String projectId,
        String title,
        String product,
        String status,
        String assigneeEmail,
        LocalDateTime dueAt
) {
}
