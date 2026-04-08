package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailBodyE2eeExternalAccessVo(
        String mode,
        String passwordHint,
        LocalDateTime expiresAt
) {
}
