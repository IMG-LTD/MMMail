package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SheetsWorkbookDetailVo(
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
        List<SheetsWorkbookSheetVo> sheets,
        List<List<String>> grid,
        List<List<String>> computedGrid,
        List<String> supportedImportFormats,
        List<String> supportedExportFormats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastOpenedAt,
        String permission,
        String scope,
        String ownerEmail,
        String ownerDisplayName,
        int collaboratorCount,
        boolean canEdit,
        boolean canManageShares,
        boolean canRestoreVersions
) {
}
