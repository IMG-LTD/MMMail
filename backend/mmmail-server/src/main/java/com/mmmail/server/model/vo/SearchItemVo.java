package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SearchItemVo(
        String moduleType,
        String resourceId,
        String title,
        String snippet,
        double score,
        LocalDateTime updatedAt,
        SearchNavigationVo navigation
) {
}
