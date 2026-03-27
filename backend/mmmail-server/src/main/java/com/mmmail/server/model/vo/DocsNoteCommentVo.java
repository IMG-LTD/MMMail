package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteCommentVo(
        String commentId,
        String authorUserId,
        String authorEmail,
        String authorDisplayName,
        String excerpt,
        String content,
        boolean resolved,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
}
