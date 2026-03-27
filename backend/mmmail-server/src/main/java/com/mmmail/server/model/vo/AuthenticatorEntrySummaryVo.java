package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuthenticatorEntrySummaryVo(
        String id,
        String issuer,
        String accountName,
        String algorithm,
        int digits,
        int periodSeconds,
        LocalDateTime updatedAt
) {
}
