package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SheetsFormulaService {

    private static final String FORMULA_PREFIX = "=";
    private static final String FORMULA_ERROR = "#ERROR!";

    public ComputationResult compute(List<List<String>> rawGrid, int rowCount, int colCount) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            SheetBuildResult sheetBuild = populateSheet(workbook.createSheet("Grid"), rawGrid, rowCount, colCount);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return buildComputedGrid(sheetBuild, evaluator, rawGrid, rowCount, colCount);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to close sheets workbook evaluator");
        }
    }

    private SheetBuildResult populateSheet(
            Sheet sheet,
            List<List<String>> rawGrid,
            int rowCount,
            int colCount
    ) {
        int formulaCellCount = 0;
        Map<String, String> hardErrors = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            List<String> sourceRow = rowIndex < rawGrid.size() ? rawGrid.get(rowIndex) : List.of();
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                Cell cell = row.createCell(colIndex);
                String rawValue = colIndex < sourceRow.size() ? normalizeValue(sourceRow.get(colIndex)) : "";
                if (isFormula(rawValue)) {
                    formulaCellCount++;
                }
                applyRawValue(cell, rawValue, hardErrors, rowIndex, colIndex);
            }
        }
        return new SheetBuildResult(sheet, formulaCellCount, hardErrors);
    }

    private void applyRawValue(
            Cell cell,
            String rawValue,
            Map<String, String> hardErrors,
            int rowIndex,
            int colIndex
    ) {
        if (!StringUtils.hasText(rawValue)) {
            cell.setBlank();
            return;
        }
        if (isFormula(rawValue)) {
            try {
                cell.setCellFormula(rawValue.substring(1));
            } catch (RuntimeException ex) {
                hardErrors.put(cellKey(rowIndex, colIndex), FORMULA_ERROR);
                cell.setBlank();
            }
            return;
        }
        Boolean booleanLiteral = parseBooleanLiteral(rawValue);
        if (booleanLiteral != null) {
            cell.setCellValue(booleanLiteral);
            return;
        }
        BigDecimal numericLiteral = parseNumericLiteral(rawValue);
        if (numericLiteral != null) {
            cell.setCellValue(numericLiteral.doubleValue());
            return;
        }
        cell.setCellValue(rawValue);
    }

    private ComputationResult buildComputedGrid(
            SheetBuildResult sheetBuild,
            FormulaEvaluator evaluator,
            List<List<String>> rawGrid,
            int rowCount,
            int colCount
    ) {
        List<List<String>> computedGrid = new ArrayList<>(rowCount);
        int computedErrorCount = 0;
        DataFormatter formatter = new DataFormatter(Locale.US);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            computedGrid.add(buildComputedRow(
                    sheetBuild,
                    evaluator,
                    formatter,
                    rawGrid,
                    rowIndex,
                    colCount
            ));
            computedErrorCount += countErrors(computedGrid.get(rowIndex));
        }
        return new ComputationResult(computedGrid, sheetBuild.formulaCellCount(), computedErrorCount, LocalDateTime.now());
    }

    private List<String> buildComputedRow(
            SheetBuildResult sheetBuild,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            List<List<String>> rawGrid,
            int rowIndex,
            int colCount
    ) {
        List<String> row = new ArrayList<>(colCount);
        Row sourceRow = sheetBuild.sheet().getRow(rowIndex);
        List<String> rawRow = rowIndex < rawGrid.size() ? rawGrid.get(rowIndex) : List.of();
        for (int colIndex = 0; colIndex < colCount; colIndex++) {
            String rawValue = colIndex < rawRow.size() ? normalizeValue(rawRow.get(colIndex)) : "";
            row.add(resolveDisplayValue(
                    sourceRow == null ? null : sourceRow.getCell(colIndex),
                    rawValue,
                    sheetBuild.hardErrors(),
                    evaluator,
                    formatter,
                    rowIndex,
                    colIndex
            ));
        }
        return row;
    }

    private String resolveDisplayValue(
            Cell cell,
            String rawValue,
            Map<String, String> hardErrors,
            FormulaEvaluator evaluator,
            DataFormatter formatter,
            int rowIndex,
            int colIndex
    ) {
        String hardError = hardErrors.get(cellKey(rowIndex, colIndex));
        if (hardError != null) {
            return hardError;
        }
        if (!isFormula(rawValue)) {
            return rawValue;
        }
        if (cell == null) {
            return FORMULA_ERROR;
        }
        try {
            return formatEvaluatedValue(cell, evaluator, formatter);
        } catch (RuntimeException ex) {
            return FORMULA_ERROR;
        }
    }

    private String formatEvaluatedValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        CellType cellType = evaluator.evaluateFormulaCell(cell);
        if (cellType == CellType.ERROR) {
            return FormulaError.forInt(cell.getErrorCellValue()).getString();
        }
        return switch (cellType) {
            case STRING -> cell.getRichStringCellValue().getString();
            case NUMERIC -> formatNumericValue(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue()).toUpperCase(Locale.ROOT);
            case BLANK -> "";
            default -> formatter.formatCellValue(cell, evaluator);
        };
    }

    private int countErrors(List<String> row) {
        int count = 0;
        for (String value : row) {
            if (value != null && value.startsWith("#")) {
                count++;
            }
        }
        return count;
    }

    private String formatNumericValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return FORMULA_ERROR;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value;
    }

    private boolean isFormula(String value) {
        return StringUtils.hasText(value) && value.startsWith(FORMULA_PREFIX) && value.length() > 1;
    }

    private Boolean parseBooleanLiteral(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private BigDecimal parseNumericLiteral(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String cellKey(int rowIndex, int colIndex) {
        return rowIndex + ":" + colIndex;
    }

    public record ComputationResult(
            List<List<String>> computedGrid,
            int formulaCellCount,
            int computedErrorCount,
            LocalDateTime computedAt
    ) {
    }

    private record SheetBuildResult(
            Sheet sheet,
            int formulaCellCount,
            Map<String, String> hardErrors
    ) {
    }
}
