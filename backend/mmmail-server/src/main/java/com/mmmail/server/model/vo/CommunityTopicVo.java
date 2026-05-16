package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record CommunityTopicVo(
        String id,
        String slug,
        String title,
        String description,
        int sortOrder,
        LocalDateTime createdAt
) {
}
