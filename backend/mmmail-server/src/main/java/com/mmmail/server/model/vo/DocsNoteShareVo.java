package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteShareVo(
        String shareId,
        String collaboratorUserId,
        String collaboratorEmail,
        String collaboratorDisplayName,
        String permission,
        LocalDateTime createdAt
) {
}
