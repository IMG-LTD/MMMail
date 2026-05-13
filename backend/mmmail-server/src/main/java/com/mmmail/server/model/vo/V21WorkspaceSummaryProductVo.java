package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21WorkspaceSummaryProductVo(
        String key,
        String label,
        String value,
        String state,
        LocalDateTime updatedAt
) {
}
