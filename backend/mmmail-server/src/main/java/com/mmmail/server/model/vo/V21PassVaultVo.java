package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21PassVaultVo(
        String id,
        String name,
        String scopeType,
        String ownerEmail,
        int itemCount,
        LocalDateTime updatedAt
) {
}
