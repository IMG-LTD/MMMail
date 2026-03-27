package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SheetsWorkbookVersionVo(
        String versionId,
        int versionNo,
        String title,
        int rowCount,
        int colCount,
        String createdByUserId,
        String createdByEmail,
        String createdByDisplayName,
        String sourceEvent,
        LocalDateTime createdAt
) {
}
