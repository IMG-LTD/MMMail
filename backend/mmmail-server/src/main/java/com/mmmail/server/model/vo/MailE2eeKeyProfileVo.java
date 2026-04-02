package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailE2eeKeyProfileVo(
        boolean enabled,
        String fingerprint,
        String algorithm,
        String publicKeyArmored,
        String encryptedPrivateKeyArmored,
        LocalDateTime keyCreatedAt
) {
}
