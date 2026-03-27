package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class SheetsSortService {

    private static final String ASC_DIRECTION = "ASC";
    private static final String DESC_DIRECTION = "DESC";
    private static final int SORT_KIND_NUMBER = 0;
    private static final int SORT_KIND_TEXT = 1;
    private static final int SORT_KIND_BLANK = 2;
    private static final int HEADER_ROW_COUNT = 1;

    public List<List<String>> sort(SortSheetRequest request) {
        validateRequest(request);
        int startRow = request.includeHeader() ? HEADER_ROW_COUNT : 0;
        if (request.rowCount() - startRow <= 1) {
            return copyGrid(request.rawGrid(), request.rowCount(), request.colCount());
        }
        List<SortableRow> sortableRows = buildSortableRows(request, startRow);
        sortableRows.sort(buildComparator(request.direction()));
        return rebuildGrid(request, startRow, sortableRows);
    }

    private void validateRequest(SortSheetRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets sort request is required");
        }
        if (request.columnIndex() == null || request.columnIndex() < 0 || request.columnIndex() >= request.colCount()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "columnIndex is out of range");
        }
        normalizeDirection(request.direction());
    }

    private List<SortableRow> buildSortableRows(SortSheetRequest request, int startRow) {
        List<SortableRow> sortableRows = new ArrayList<>(request.rowCount() - startRow);
        for (int rowIndex = startRow; rowIndex < request.rowCount(); rowIndex++) {
            sortableRows.add(new SortableRow(
                    rowIndex,
                    copyRow(request.rawGrid(), rowIndex, request.colCount()),
                    parseSortCell(cellValueAt(request.computedGrid(), rowIndex, request.columnIndex()))
            ));
        }
        return sortableRows;
    }

    private Comparator<SortableRow> buildComparator(String direction) {
        boolean descending = DESC_DIRECTION.equals(normalizeDirection(direction));
        return (left, right) -> compareRows(left, right, descending);
    }

    private int compareRows(SortableRow left, SortableRow right, boolean descending) {
        int kindCompare = Integer.compare(left.sortCell().kind(), right.sortCell().kind());
        if (kindCompare != 0) {
            return kindCompare;
        }
        int valueCompare = compareSortCellValue(left.sortCell(), right.sortCell());
        if (descending) {
            valueCompare = -valueCompare;
        }
        if (valueCompare != 0) {
            return valueCompare;
        }
        return Integer.compare(left.originalIndex(), right.originalIndex());
    }

    private int compareSortCellValue(SortCell left, SortCell right) {
        if (left.kind() == SORT_KIND_NUMBER) {
            return left.numericValue().compareTo(right.numericValue());
        }
        if (left.kind() == SORT_KIND_TEXT) {
            int insensitive = String.CASE_INSENSITIVE_ORDER.compare(left.textValue(), right.textValue());
            if (insensitive != 0) {
                return insensitive;
            }
            return left.textValue().compareTo(right.textValue());
        }
        return 0;
    }

    private List<List<String>> rebuildGrid(SortSheetRequest request, int startRow, List<SortableRow> sortableRows) {
        List<List<String>> rebuilt = new ArrayList<>(request.rowCount());
        if (startRow == HEADER_ROW_COUNT) {
            rebuilt.add(copyRow(request.rawGrid(), 0, request.colCount()));
        }
        for (SortableRow sortableRow : sortableRows) {
            rebuilt.add(sortableRow.rawRow());
        }
        return rebuilt;
    }

    private List<List<String>> copyGrid(List<List<String>> source, int rowCount, int colCount) {
        List<List<String>> copied = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            copied.add(copyRow(source, rowIndex, colCount));
        }
        return copied;
    }

    private List<String> copyRow(List<List<String>> source, int rowIndex, int colCount) {
        List<String> row = new ArrayList<>(colCount);
        for (int colIndex = 0; colIndex < colCount; colIndex++) {
            row.add(cellValueAt(source, rowIndex, colIndex));
        }
        return row;
    }

    private String cellValueAt(List<List<String>> grid, int rowIndex, int colIndex) {
        if (grid == null || rowIndex < 0 || rowIndex >= grid.size()) {
            return "";
        }
        List<String> row = grid.get(rowIndex);
        if (row == null || colIndex < 0 || colIndex >= row.size()) {
            return "";
        }
        String value = row.get(colIndex);
        return value == null ? "" : value;
    }

    private SortCell parseSortCell(String value) {
        if (!StringUtils.hasText(value)) {
            return new SortCell(SORT_KIND_BLANK, null, "");
        }
        String trimmed = value.trim();
        BigDecimal numericValue = parseNumericValue(trimmed);
        if (numericValue != null) {
            return new SortCell(SORT_KIND_NUMBER, numericValue, trimmed);
        }
        return new SortCell(SORT_KIND_TEXT, null, trimmed);
    }

    private BigDecimal parseNumericValue(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "direction is required");
        }
        String normalized = direction.trim().toUpperCase(Locale.ROOT);
        if (ASC_DIRECTION.equals(normalized) || DESC_DIRECTION.equals(normalized)) {
            return normalized;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "direction is invalid");
    }

    public record SortSheetRequest(
            List<List<String>> rawGrid,
            List<List<String>> computedGrid,
            int rowCount,
            int colCount,
            Integer columnIndex,
            String direction,
            boolean includeHeader
    ) {
    }

    private record SortableRow(int originalIndex, List<String> rawRow, SortCell sortCell) {
    }

    private record SortCell(int kind, BigDecimal numericValue, String textValue) {
    }
}
