package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostVo(
        String id,
        Long authorUserId,
        String topicId,
        String title,
        String bodyMd,
        String bodyHtml,
        List<String> tags,
        int likeCount,
        int commentCount,
        int viewCount,
        boolean pinned,
        boolean locked,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
