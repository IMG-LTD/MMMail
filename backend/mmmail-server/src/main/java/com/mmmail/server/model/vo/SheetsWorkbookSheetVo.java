package com.mmmail.server.model.vo;

import java.util.List;

public record SheetsWorkbookSheetVo(
        String id,
        String name,
        int rowCount,
        int colCount,
        int frozenRowCount,
        int frozenColCount,
        int filledCellCount,
        int formulaCellCount,
        int computedErrorCount,
        List<List<String>> grid,
        List<List<String>> computedGrid
) {
}
