package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record TrustedSenderVo(
        String id,
        String email,
        LocalDateTime createdAt
) {
}
