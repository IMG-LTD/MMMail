package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailExternalAccountSyncVo(
        String accountId,
        String syncStatus,
        int imported,
        int skipped,
        LocalDateTime lastSyncAt,
        String lastError
) {
}
