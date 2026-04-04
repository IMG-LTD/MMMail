package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailE2eeRecoveryVo(
        boolean enabled,
        String encryptedPrivateKeyArmored,
        LocalDateTime updatedAt
) {
}
