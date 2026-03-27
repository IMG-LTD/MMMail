package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassSharedVaultSummaryVo(
        String id,
        String orgId,
        String name,
        String description,
        String accessRole,
        int memberCount,
        int itemCount,
        String createdByEmail,
        LocalDateTime updatedAt
) {
}
