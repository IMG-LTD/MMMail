package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteSummaryVo(
        String id,
        String title,
        LocalDateTime updatedAt,
        String permission,
        String scope,
        int currentVersion,
        String ownerEmail,
        String ownerDisplayName,
        int collaboratorCount
) {
}
