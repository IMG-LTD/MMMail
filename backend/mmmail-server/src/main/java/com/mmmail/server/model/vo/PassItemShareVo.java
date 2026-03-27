package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassItemShareVo(
        String id,
        String itemId,
        String collaboratorUserId,
        String collaboratorEmail,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
