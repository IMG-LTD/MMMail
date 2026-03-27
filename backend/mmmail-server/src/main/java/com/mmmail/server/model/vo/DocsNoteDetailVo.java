package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteDetailVo(
        String id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int currentVersion,
        String permission,
        boolean shared,
        String ownerEmail,
        String ownerDisplayName,
        int collaboratorCount,
        long syncCursor,
        String syncVersion
) {
}
