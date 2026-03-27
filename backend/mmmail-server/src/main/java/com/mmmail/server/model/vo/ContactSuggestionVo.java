package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record ContactSuggestionVo(
        String email,
        String displayName,
        boolean isFavorite,
        String source,
        LocalDateTime lastContactAt,
        long messageCount
) {
}
