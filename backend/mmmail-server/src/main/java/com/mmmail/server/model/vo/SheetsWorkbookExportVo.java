package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SheetsWorkbookExportVo(
        String fileName,
        String format,
        String content,
        int formulaCellCount,
        int computedErrorCount,
        LocalDateTime exportedAt
) {
}
