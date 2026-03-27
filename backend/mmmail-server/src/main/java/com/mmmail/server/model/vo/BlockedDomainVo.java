package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record BlockedDomainVo(
        String id,
        String domain,
        LocalDateTime createdAt
) {
}
