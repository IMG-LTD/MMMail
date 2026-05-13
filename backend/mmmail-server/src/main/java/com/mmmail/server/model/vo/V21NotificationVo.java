package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record V21NotificationVo(
        String id,
        String title,
        String body,
        String product,
        String severity,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
