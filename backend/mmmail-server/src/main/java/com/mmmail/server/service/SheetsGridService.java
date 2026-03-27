package com.mmmail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.SheetCellEditInput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SheetsGridService {

    public static final int DEFAULT_ROW_COUNT = 20;
    public static final int DEFAULT_COL_COUNT = 12;
    private static final int MAX_ROW_COUNT = 200;
    private static final int MAX_COL_COUNT = 52;
    private static final int MAX_EDIT_BATCH = 500;
    private static final int MAX_CELL_LENGTH = 4_000;

    private final ObjectMapper objectMapper;

    public SheetsGridService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int normalizeRowCount(Integer rowCount) {
        return normalizeDimension(rowCount, DEFAULT_ROW_COUNT, MAX_ROW_COUNT, "rowCount");
    }

    public int normalizeColCount(Integer colCount) {
        return normalizeDimension(colCount, DEFAULT_COL_COUNT, MAX_COL_COUNT, "colCount");
    }

    public List<List<String>> createEmptyGrid(int rowCount, int colCount) {
        List<List<String>> grid = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<String> row = new ArrayList<>(colCount);
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                row.add("");
            }
            grid.add(row);
        }
        return grid;
    }

    public List<List<String>> readGrid(String gridJson, int rowCount, int colCount) {
        if (gridJson == null || gridJson.isBlank()) {
            return createEmptyGrid(rowCount, colCount);
        }
        try {
            List<List<String>> parsed = objectMapper.readValue(gridJson, new TypeReference<>() {
            });
            return normalizeGrid(parsed, rowCount, colCount);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse sheets workbook grid");
        }
    }

    public String writeGrid(List<List<String>> grid) {
        try {
            return objectMapper.writeValueAsString(grid);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize sheets workbook grid");
        }
    }

    public List<List<String>> applyEdits(
            List<List<String>> grid,
            int rowCount,
            int colCount,
            List<SheetCellEditInput> edits
    ) {
        validateEditBatch(edits);
        List<List<String>> nextGrid = normalizeGrid(grid, rowCount, colCount);
        for (SheetCellEditInput edit : edits) {
            int rowIndex = edit.rowIndex();
            int colIndex = edit.colIndex();
            validateCellPosition(rowIndex, rowCount, "rowIndex");
            validateCellPosition(colIndex, colCount, "colIndex");
            nextGrid.get(rowIndex).set(colIndex, normalizeCellValue(edit.value()));
        }
        return nextGrid;
    }

    public int countFilledCells(List<List<String>> grid) {
        int count = 0;
        for (List<String> row : grid) {
            for (String value : row) {
                if (value != null && !value.isBlank()) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<List<String>> normalizeImportedGrid(List<List<String>> grid, int rowCount, int colCount) {
        return normalizeGrid(grid, rowCount, colCount);
    }

    private int normalizeDimension(Integer value, int defaultValue, int maxValue, String fieldName) {
        int safeValue = value == null ? defaultValue : value;
        if (safeValue < 1 || safeValue > maxValue) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is out of range");
        }
        return safeValue;
    }

    private List<List<String>> normalizeGrid(List<List<String>> source, int rowCount, int colCount) {
        List<List<String>> normalized = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<String> sourceRow = source != null && rowIndex < source.size() && source.get(rowIndex) != null
                    ? source.get(rowIndex)
                    : List.of();
            normalized.add(normalizeRow(sourceRow, colCount));
        }
        return normalized;
    }

    private List<String> normalizeRow(List<String> sourceRow, int colCount) {
        List<String> normalized = new ArrayList<>(colCount);
        for (int colIndex = 0; colIndex < colCount; colIndex++) {
            String sourceValue = colIndex < sourceRow.size() ? sourceRow.get(colIndex) : "";
            normalized.add(normalizeCellValue(sourceValue));
        }
        return normalized;
    }

    private void validateEditBatch(List<SheetCellEditInput> edits) {
        if (edits == null || edits.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one cell edit is required");
        }
        if (edits.size() > MAX_EDIT_BATCH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Too many cell edits in a single request");
        }
    }

    private void validateCellPosition(int value, int maxExclusive, String fieldName) {
        if (value < 0 || value >= maxExclusive) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is out of range");
        }
    }

    private String normalizeCellValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.length() > MAX_CELL_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cell value is too long");
        }
        return safeValue;
    }
}
