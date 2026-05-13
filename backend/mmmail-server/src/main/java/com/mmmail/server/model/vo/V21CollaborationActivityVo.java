package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21CollaborationActivityVo(
        String id,
        String title,
        String product,
        LocalDateTime occurredAt
) {
}
