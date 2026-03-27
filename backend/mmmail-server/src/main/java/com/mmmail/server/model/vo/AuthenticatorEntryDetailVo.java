package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuthenticatorEntryDetailVo(
        String id,
        String issuer,
        String accountName,
        String secretCiphertext,
        String algorithm,
        int digits,
        int periodSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
