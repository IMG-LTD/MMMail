package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record BlockedSenderVo(
        String id,
        String email,
        LocalDateTime createdAt
) {
}
