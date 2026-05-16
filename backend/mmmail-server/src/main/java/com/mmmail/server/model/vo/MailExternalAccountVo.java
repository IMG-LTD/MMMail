package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailExternalAccountVo(
        String accountId,
        String provider,
        String authMode,
        String email,
        String username,
        MailExternalServerVo imap,
        MailExternalServerVo smtp,
        String syncStatus,
        LocalDateTime lastSyncAt,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
