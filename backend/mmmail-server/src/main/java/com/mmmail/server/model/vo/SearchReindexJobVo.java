package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SearchReindexJobVo(
        String jobId,
        String moduleType,
        String status,
        int processed,
        int total,
        int errors,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
