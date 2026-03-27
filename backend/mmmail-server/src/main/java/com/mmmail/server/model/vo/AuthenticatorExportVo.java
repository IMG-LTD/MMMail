package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record AuthenticatorExportVo(
        String format,
        String fileName,
        String content,
        int entryCount,
        LocalDateTime exportedAt
) {
}
