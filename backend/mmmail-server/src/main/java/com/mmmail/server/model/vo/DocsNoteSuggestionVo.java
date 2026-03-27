package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNoteSuggestionVo(
        String suggestionId,
        String authorUserId,
        String authorEmail,
        String authorDisplayName,
        String status,
        int selectionStart,
        int selectionEnd,
        String originalText,
        String replacementText,
        int baseVersion,
        String resolvedByUserId,
        String resolvedByEmail,
        String resolvedByDisplayName,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
}
