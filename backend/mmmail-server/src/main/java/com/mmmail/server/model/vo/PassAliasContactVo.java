package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassAliasContactVo(
        String id,
        String aliasId,
        String targetUserId,
        String targetEmail,
        String displayName,
        String note,
        String reverseAliasEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
