package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationProjectVo(
        String id,
        String name,
        String product,
        String status,
        int taskCount,
        LocalDateTime updatedAt
) {
}
