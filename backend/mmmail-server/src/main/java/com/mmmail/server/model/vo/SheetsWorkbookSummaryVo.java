package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record SheetsWorkbookSummaryVo(
        String id,
        String title,
        int rowCount,
        int colCount,
        int filledCellCount,
        int formulaCellCount,
        int computedErrorCount,
        int currentVersion,
        int sheetCount,
        String activeSheetId,
        LocalDateTime updatedAt,
        LocalDateTime lastOpenedAt,
        String permission,
        String scope,
        String ownerEmail,
        String ownerDisplayName,
        int collaboratorCount,
        boolean canEdit
) {
}
