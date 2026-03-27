package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record UserSessionVo(
        String id,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean current
) {
}
