package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record ContactItemVo(
        String id,
        String displayName,
        String email,
        String note,
        boolean isFavorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
