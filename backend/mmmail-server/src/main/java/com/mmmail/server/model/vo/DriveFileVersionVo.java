package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DriveFileVersionVo(
        String id,
        String itemId,
        int versionNo,
        String mimeType,
        long sizeBytes,
        String checksum,
        LocalDateTime createdAt
) {
}
