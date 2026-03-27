package com.mmmail.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.vo.SheetsWorkbookExportVo;
import com.mmmail.server.model.vo.SheetsWorkbookSheetVo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SheetsImportExportService {

    public static final List<String> SUPPORTED_IMPORT_FORMATS = List.of("CSV", "TSV", "XLSX");
    public static final List<String> SUPPORTED_EXPORT_FORMATS = List.of("CSV", "TSV", "JSON");
    private static final int BLANK_IMPORTED_DIMENSION = 1;
    private static final int MAX_IMPORT_BYTES = 2 * 1024 * 1024;
    private static final Pattern NON_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9-_]+");

    private final SheetsGridService sheetsGridService;
    private final ObjectMapper objectMapper;

    public SheetsImportExportService(SheetsGridService sheetsGridService, ObjectMapper objectMapper) {
        this.sheetsGridService = sheetsGridService;
        this.objectMapper = objectMapper;
    }

    public ImportedWorkbook importWorkbook(MultipartFile file, String requestedTitle) {
        validateImportFile(file);
        String format = detectImportFormat(file.getOriginalFilename());
        List<ImportedSheet> importedSheets = switch (format) {
            case "CSV" -> List.of(parseDelimitedSheet(file, CSVFormat.DEFAULT));
            case "TSV" -> List.of(parseDelimitedSheet(file, CSVFormat.TDF));
            case "XLSX" -> parseXlsxSheets(file);
            default -> throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Unsupported sheets import format");
        };
        return new ImportedWorkbook(resolveTitle(requestedTitle, file.getOriginalFilename()), importedSheets, format);
    }

    public SheetsWorkbookExportVo exportWorkbook(
            SheetsWorkbook workbook,
            List<SheetsWorkbookSheetVo> sheets,
            String activeSheetId,
            String format
    ) {
        String normalizedFormat = normalizeExportFormat(format);
        SheetsWorkbookSheetVo activeSheet = resolveActiveSheet(sheets, activeSheetId);
        String content = switch (normalizedFormat) {
            case "CSV" -> renderDelimited(activeSheet.computedGrid(), CSVFormat.DEFAULT);
            case "TSV" -> renderDelimited(activeSheet.computedGrid(), CSVFormat.TDF);
            case "JSON" -> renderJson(workbook, sheets, activeSheet);
            default -> throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Unsupported sheets export format");
        };
        return new SheetsWorkbookExportVo(
                buildFileName(workbook.getTitle(), normalizedFormat),
                normalizedFormat,
                content,
                activeSheet.formulaCellCount(),
                activeSheet.computedErrorCount(),
                LocalDateTime.now()
        );
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Sheets import file is required");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Sheets import file is too large");
        }
    }

    private String detectImportFormat(String originalFilename) {
        String normalized = originalFilename == null ? "" : originalFilename.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".csv")) {
            return "CSV";
        }
        if (normalized.endsWith(".tsv")) {
            return "TSV";
        }
        if (normalized.endsWith(".xlsx")) {
            return "XLSX";
        }
        throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Only CSV, TSV, and XLSX imports are supported");
    }

    private ImportedSheet parseDelimitedSheet(MultipartFile file, CSVFormat format) {
        ParsedGrid parsedGrid = parseDelimited(file, format);
        return new ImportedSheet("Sheet 1", parsedGrid.rowCount(), parsedGrid.colCount(), parsedGrid.grid());
    }

    private ParsedGrid parseDelimited(MultipartFile file, CSVFormat format) {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<List<String>> rows = new ArrayList<>();
            int colCount = 0;
            for (org.apache.commons.csv.CSVRecord record : parser) {
                List<String> row = new ArrayList<>(record.size());
                for (String value : record) {
                    row.add(value == null ? "" : value);
                }
                rows.add(row);
                colCount = Math.max(colCount, row.size());
            }
            return buildParsedGrid(rows, colCount);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Failed to parse sheets import file");
        }
    }

    private List<ImportedSheet> parseXlsxSheets(MultipartFile file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() < 1) {
                throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "XLSX workbook has no sheets");
            }
            List<ImportedSheet> importedSheets = new ArrayList<>(workbook.getNumberOfSheets());
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                importedSheets.add(parseXlsxSheet(workbook.getSheetAt(index), index));
            }
            return importedSheets;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Failed to parse XLSX workbook");
        }
    }

    private ImportedSheet parseXlsxSheet(Sheet sheet, int sheetIndex) {
        DataFormatter formatter = new DataFormatter(Locale.US);
        List<List<String>> rows = new ArrayList<>();
        int colCount = 0;
        int lastRowIndex = sheet.getLastRowNum();
        for (int rowIndex = 0; rowIndex <= lastRowIndex; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            List<String> parsedRow = row == null ? List.of() : parseXlsxRow(row, formatter);
            rows.add(parsedRow);
            colCount = Math.max(colCount, parsedRow.size());
        }
        ParsedGrid parsedGrid = buildOptionalParsedGrid(rows, colCount);
        String name = StringUtils.hasText(sheet.getSheetName()) ? sheet.getSheetName() : "Sheet " + (sheetIndex + 1);
        return new ImportedSheet(name, parsedGrid.rowCount(), parsedGrid.colCount(), parsedGrid.grid());
    }

    private List<String> parseXlsxRow(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return values;
        }
        for (int colIndex = 0; colIndex < lastCellNum; colIndex++) {
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(readXlsxCell(cell, formatter));
        }
        trimTrailingBlanks(values);
        return values;
    }

    private String readXlsxCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return "=" + cell.getCellFormula();
        }
        return formatter.formatCellValue(cell);
    }

    private ParsedGrid buildParsedGrid(List<List<String>> rows, int colCount) {
        trimTrailingEmptyRows(rows);
        if (rows.isEmpty() || colCount == 0) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Sheets import file has no usable grid data");
        }
        return new ParsedGrid(rows, rows.size(), colCount);
    }

    private ParsedGrid buildOptionalParsedGrid(List<List<String>> rows, int colCount) {
        trimTrailingEmptyRows(rows);
        if (rows.isEmpty() || colCount == 0) {
            List<List<String>> blankGrid = sheetsGridService.createEmptyGrid(BLANK_IMPORTED_DIMENSION, BLANK_IMPORTED_DIMENSION);
            return new ParsedGrid(blankGrid, BLANK_IMPORTED_DIMENSION, BLANK_IMPORTED_DIMENSION);
        }
        return new ParsedGrid(rows, rows.size(), colCount);
    }

    private void trimTrailingEmptyRows(List<List<String>> rows) {
        while (!rows.isEmpty() && rows.get(rows.size() - 1).stream().allMatch(value -> value == null || value.isBlank())) {
            rows.remove(rows.size() - 1);
        }
    }

    private void trimTrailingBlanks(List<String> row) {
        while (!row.isEmpty() && !StringUtils.hasText(row.get(row.size() - 1))) {
            row.remove(row.size() - 1);
        }
    }

    private String normalizeExportFormat(String format) {
        if (!StringUtils.hasText(format)) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Sheets export format is required");
        }
        String normalized = format.trim().toUpperCase(Locale.ROOT);
        if (SUPPORTED_EXPORT_FORMATS.contains(normalized)) {
            return normalized;
        }
        throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Unsupported sheets export format");
    }

    private SheetsWorkbookSheetVo resolveActiveSheet(List<SheetsWorkbookSheetVo> sheets, String activeSheetId) {
        for (SheetsWorkbookSheetVo sheet : sheets) {
            if (sheet.id().equals(activeSheetId)) {
                return sheet;
            }
        }
        throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Sheets active sheet is missing");
    }

    private String renderDelimited(List<List<String>> grid, CSVFormat format) {
        try (StringWriter writer = new StringWriter(); CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (List<String> row : grid) {
                printer.printRecord(row);
            }
            printer.flush();
            return writer.toString();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Failed to render sheets export content");
        }
    }

    private String renderJson(
            SheetsWorkbook workbook,
            List<SheetsWorkbookSheetVo> sheets,
            SheetsWorkbookSheetVo activeSheet
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workbookId", String.valueOf(workbook.getId()));
        payload.put("title", workbook.getTitle());
        payload.put("rowCount", activeSheet.rowCount());
        payload.put("colCount", activeSheet.colCount());
        payload.put("currentVersion", workbook.getCurrentVersion());
        payload.put("activeSheetId", activeSheet.id());
        payload.put("formulaCellCount", activeSheet.formulaCellCount());
        payload.put("computedErrorCount", activeSheet.computedErrorCount());
        payload.put("grid", activeSheet.grid());
        payload.put("computedGrid", activeSheet.computedGrid());
        payload.put("sheets", sheets);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_EXPORT_INVALID, "Failed to serialize sheets JSON export");
        }
    }

    private String buildFileName(String title, String format) {
        String normalizedTitle = NON_FILENAME_CHARS.matcher(title.trim().replace(' ', '-')).replaceAll("");
        String baseName = StringUtils.hasText(normalizedTitle) ? normalizedTitle.toLowerCase(Locale.ROOT) : "workbook";
        String extension = format.toLowerCase(Locale.ROOT);
        return baseName + "-" + LocalDateTime.now().toLocalDate() + "." + extension;
    }

    private String resolveTitle(String requestedTitle, String originalFilename) {
        if (StringUtils.hasText(requestedTitle)) {
            return requestedTitle.trim();
        }
        String fileName = originalFilename == null ? "Imported workbook" : originalFilename.trim();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    public record ImportedWorkbook(String title, List<ImportedSheet> sheets, String sourceFormat) {
    }

    public record ImportedSheet(String name, int rowCount, int colCount, List<List<String>> grid) {
    }

    private record ParsedGrid(List<List<String>> grid, int rowCount, int colCount) {
    }
}
