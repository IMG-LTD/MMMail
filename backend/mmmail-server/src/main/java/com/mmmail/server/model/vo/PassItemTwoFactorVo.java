package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassItemTwoFactorVo(
        boolean enabled,
        String issuer,
        String accountName,
        String algorithm,
        Integer digits,
        Integer periodSeconds,
        LocalDateTime updatedAt
) {
}
