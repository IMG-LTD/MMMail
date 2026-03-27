package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record TrustedDomainVo(
        String id,
        String domain,
        LocalDateTime createdAt
) {
}
