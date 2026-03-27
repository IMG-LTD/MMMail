package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceFileVersionVo(
        String id,
        String itemId,
        int versionNo,
        String mimeType,
        long sizeBytes,
        String checksum,
        String ownerEmail,
        LocalDateTime createdAt
) {
}
