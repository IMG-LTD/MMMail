package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveCollaboratorShareVo(
        String shareId,
        String collaboratorUserId,
        String collaboratorEmail,
        String collaboratorDisplayName,
        String permission,
        String responseStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
