package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CollabSnapshotVo(
        String resourceType,
        String resourceId,
        Integer version,
        String snapshotBase64,
        LocalDateTime updatedAt
) {
}
