package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuthenticatorBackupVo(
        String fileName,
        String content,
        int entryCount,
        String encryption,
        LocalDateTime exportedAt
) {
}
