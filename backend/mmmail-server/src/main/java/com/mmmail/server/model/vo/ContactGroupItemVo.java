package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record ContactGroupItemVo(
        String id,
        String name,
        String description,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
