package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentVo(
        String id,
        String postId,
        String parentCommentId,
        Long authorUserId,
        String bodyMd,
        String bodyHtml,
        String status,
        LocalDateTime createdAt,
        List<CommunityCommentVo> replies
) {
}
