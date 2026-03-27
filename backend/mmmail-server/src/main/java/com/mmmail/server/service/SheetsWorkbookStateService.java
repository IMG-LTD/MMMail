package com.mmmail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.SheetsWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SheetsWorkbookStateService {

    private static final int EMPTY_IMPORTED_DIMENSION = 1;
    private static final int MAX_SHEET_NAME_LENGTH = 128;
    private static final String DEFAULT_SHEET_PREFIX = "Sheet";

    private final ObjectMapper objectMapper;
    private final SheetsGridService sheetsGridService;

    public SheetsWorkbookStateService(ObjectMapper objectMapper, SheetsGridService sheetsGridService) {
        this.objectMapper = objectMapper;
        this.sheetsGridService = sheetsGridService;
    }

    public WorkbookState createInitialState(int rowCount, int colCount) {
        SheetState sheet = createEmptySheet(defaultSheetName(1), rowCount, colCount);
        return new WorkbookState(List.of(sheet), sheet.id());
    }

    public WorkbookState createImportedState(List<SheetSeed> seeds) {
        validateSeeds(seeds);
        List<SheetState> sheets = new ArrayList<>(seeds.size());
        Set<String> usedIds = new HashSet<>();
        for (int index = 0; index < seeds.size(); index++) {
            sheets.add(buildImportedSheet(seeds.get(index), index + 1, usedIds));
        }
        return new WorkbookState(List.copyOf(sheets), sheets.get(0).id());
    }

    public WorkbookState readState(SheetsWorkbook workbook) {
        if (!StringUtils.hasText(workbook.getSheetsJson())) {
            return buildLegacyState(workbook);
        }
        List<SheetState> sheets = parseStoredSheets(workbook.getSheetsJson());
        String activeSheetId = resolveActiveSheetId(workbook.getActiveSheetId(), sheets);
        return new WorkbookState(List.copyOf(sheets), activeSheetId);
    }

    public void syncWorkbook(SheetsWorkbook workbook, WorkbookState state) {
        SheetState activeSheet = requireSheet(state, state.activeSheetId());
        workbook.setSheetsJson(writeSheetsJson(state.sheets()));
        workbook.setActiveSheetId(activeSheet.id());
        workbook.setRowCount(activeSheet.rowCount());
        workbook.setColCount(activeSheet.colCount());
        workbook.setGridJson(sheetsGridService.writeGrid(activeSheet.grid()));
    }

    public SheetState activeSheet(WorkbookState state) {
        return requireSheet(state, state.activeSheetId());
    }

    public SheetState requireSheet(WorkbookState state, String sheetId) {
        for (SheetState sheet : state.sheets()) {
            if (sheet.id().equals(sheetId)) {
                return sheet;
            }
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets sheet not found");
    }

    public String resolveSheetId(WorkbookState state, String requestedSheetId) {
        if (!StringUtils.hasText(requestedSheetId)) {
            return state.activeSheetId();
        }
        return requireSheet(state, requestedSheetId.trim()).id();
    }

    public WorkbookState addSheet(WorkbookState state, String requestedName, Integer rowCount, Integer colCount) {
        SheetState activeSheet = activeSheet(state);
        int nextRowCount = rowCount == null ? activeSheet.rowCount() : sheetsGridService.normalizeRowCount(rowCount);
        int nextColCount = colCount == null ? activeSheet.colCount() : sheetsGridService.normalizeColCount(colCount);
        String nextName = resolveCreateSheetName(requestedName, state.sheets());
        SheetState sheet = createEmptySheet(nextName, nextRowCount, nextColCount);
        List<SheetState> sheets = new ArrayList<>(state.sheets());
        sheets.add(sheet);
        return new WorkbookState(List.copyOf(sheets), sheet.id());
    }

    public WorkbookState renameSheet(WorkbookState state, String sheetId, String requestedName) {
        SheetState targetSheet = requireSheet(state, sheetId);
        String nextName = normalizeRequiredSheetName(requestedName);
        List<SheetState> sheets = replaceSheet(state.sheets(), targetSheet.withName(nextName));
        return new WorkbookState(List.copyOf(sheets), state.activeSheetId());
    }

    public WorkbookState deleteSheet(WorkbookState state, String sheetId) {
        if (state.sheets().size() == 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one sheet must remain in the workbook");
        }
        int removedIndex = findSheetIndex(state.sheets(), sheetId);
        List<SheetState> sheets = new ArrayList<>(state.sheets());
        sheets.remove(removedIndex);
        String nextActiveSheetId = resolveNextActiveSheetId(state, sheetId, sheets, removedIndex);
        return new WorkbookState(List.copyOf(sheets), nextActiveSheetId);
    }

    public WorkbookState setActiveSheet(WorkbookState state, String sheetId) {
        String nextActiveSheetId = requireSheet(state, sheetId).id();
        return new WorkbookState(state.sheets(), nextActiveSheetId);
    }

    public WorkbookState updateSheetGrid(WorkbookState state, String sheetId, List<List<String>> grid) {
        SheetState targetSheet = requireSheet(state, sheetId);
        List<List<String>> normalizedGrid = sheetsGridService.normalizeImportedGrid(grid, targetSheet.rowCount(), targetSheet.colCount());
        List<SheetState> sheets = replaceSheet(state.sheets(), targetSheet.withGrid(normalizedGrid));
        return new WorkbookState(List.copyOf(sheets), state.activeSheetId());
    }

    public WorkbookState updateSheetFreeze(
            WorkbookState state,
            String sheetId,
            Integer frozenRowCount,
            Integer frozenColCount
    ) {
        SheetState targetSheet = requireSheet(state, sheetId);
        int nextFrozenRowCount = normalizeFreezeCount(frozenRowCount, targetSheet.rowCount(), "frozenRowCount");
        int nextFrozenColCount = normalizeFreezeCount(frozenColCount, targetSheet.colCount(), "frozenColCount");
        List<SheetState> sheets = replaceSheet(state.sheets(), targetSheet.withFreeze(nextFrozenRowCount, nextFrozenColCount));
        return new WorkbookState(List.copyOf(sheets), state.activeSheetId());
    }

    private void validateSeeds(List<SheetSeed> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_IMPORT_INVALID, "Sheets workbook import has no sheets");
        }
    }

    private SheetState buildImportedSheet(SheetSeed seed, int sheetNumber, Set<String> usedIds) {
        int rowCount = normalizeImportedRowCount(seed.rowCount());
        int colCount = normalizeImportedColCount(seed.colCount());
        String name = normalizeImportedSheetName(seed.name(), sheetNumber);
        String id = uniqueSheetId(usedIds);
        int frozenRowCount = normalizeFreezeCount(seed.frozenRowCount(), rowCount, "frozenRowCount");
        int frozenColCount = normalizeFreezeCount(seed.frozenColCount(), colCount, "frozenColCount");
        List<List<String>> grid = buildImportedGrid(seed.grid(), rowCount, colCount);
        return new SheetState(id, name, rowCount, colCount, frozenRowCount, frozenColCount, grid);
    }

    private List<List<String>> buildImportedGrid(List<List<String>> grid, int rowCount, int colCount) {
        if (grid == null || grid.isEmpty()) {
            return sheetsGridService.createEmptyGrid(rowCount, colCount);
        }
        return sheetsGridService.normalizeImportedGrid(grid, rowCount, colCount);
    }

    private WorkbookState buildLegacyState(SheetsWorkbook workbook) {
        int rowCount = sheetsGridService.normalizeRowCount(workbook.getRowCount());
        int colCount = sheetsGridService.normalizeColCount(workbook.getColCount());
        String activeSheetId = StringUtils.hasText(workbook.getActiveSheetId()) ? workbook.getActiveSheetId().trim() : generateSheetId();
        List<List<String>> grid = sheetsGridService.readGrid(workbook.getGridJson(), rowCount, colCount);
        SheetState legacySheet = new SheetState(activeSheetId, defaultSheetName(1), rowCount, colCount, 0, 0, grid);
        return new WorkbookState(List.of(legacySheet), activeSheetId);
    }

    private List<SheetState> parseStoredSheets(String sheetsJson) {
        try {
            List<StoredSheet> storedSheets = objectMapper.readValue(sheetsJson, new TypeReference<>() {
            });
            return normalizeStoredSheets(storedSheets);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse sheets workbook state");
        }
    }

    private List<SheetState> normalizeStoredSheets(List<StoredSheet> storedSheets) {
        if (storedSheets == null || storedSheets.isEmpty()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Sheets workbook state has no sheets");
        }
        List<SheetState> normalizedSheets = new ArrayList<>(storedSheets.size());
        Set<String> usedIds = new HashSet<>();
        for (int index = 0; index < storedSheets.size(); index++) {
            normalizedSheets.add(normalizeStoredSheet(storedSheets.get(index), index + 1, usedIds));
        }
        return normalizedSheets;
    }

    private SheetState normalizeStoredSheet(StoredSheet storedSheet, int sheetNumber, Set<String> usedIds) {
        int rowCount = sheetsGridService.normalizeRowCount(storedSheet.rowCount());
        int colCount = sheetsGridService.normalizeColCount(storedSheet.colCount());
        String name = normalizeImportedSheetName(storedSheet.name(), sheetNumber);
        String id = normalizeStoredSheetId(storedSheet.id(), usedIds);
        int frozenRowCount = normalizeFreezeCount(storedSheet.frozenRowCount(), rowCount, "frozenRowCount");
        int frozenColCount = normalizeFreezeCount(storedSheet.frozenColCount(), colCount, "frozenColCount");
        List<List<String>> grid = buildImportedGrid(storedSheet.grid(), rowCount, colCount);
        return new SheetState(id, name, rowCount, colCount, frozenRowCount, frozenColCount, grid);
    }

    private String normalizeStoredSheetId(String candidateId, Set<String> usedIds) {
        if (StringUtils.hasText(candidateId) && usedIds.add(candidateId.trim())) {
            return candidateId.trim();
        }
        return uniqueSheetId(usedIds);
    }

    private String resolveActiveSheetId(String candidateId, List<SheetState> sheets) {
        if (StringUtils.hasText(candidateId)) {
            String normalizedId = candidateId.trim();
            for (SheetState sheet : sheets) {
                if (sheet.id().equals(normalizedId)) {
                    return normalizedId;
                }
            }
        }
        return sheets.get(0).id();
    }

    private String writeSheetsJson(List<SheetState> sheets) {
        List<StoredSheet> payload = sheets.stream()
                .map(sheet -> new StoredSheet(
                        sheet.id(),
                        sheet.name(),
                        sheet.rowCount(),
                        sheet.colCount(),
                        sheet.frozenRowCount(),
                        sheet.frozenColCount(),
                        sheet.grid()
                ))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize sheets workbook state");
        }
    }

    private SheetState createEmptySheet(String name, int rowCount, int colCount) {
        return new SheetState(
                generateSheetId(),
                normalizeImportedSheetName(name, 1),
                rowCount,
                colCount,
                0,
                0,
                sheetsGridService.createEmptyGrid(rowCount, colCount)
        );
    }

    private String resolveCreateSheetName(String requestedName, List<SheetState> sheets) {
        if (StringUtils.hasText(requestedName)) {
            return normalizeRequiredSheetName(requestedName);
        }
        return nextDefaultSheetName(sheets);
    }

    private String normalizeRequiredSheetName(String requestedName) {
        if (!StringUtils.hasText(requestedName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets sheet name is required");
        }
        String normalizedName = requestedName.trim();
        if (normalizedName.length() > MAX_SHEET_NAME_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets sheet name is too long");
        }
        return normalizedName;
    }

    private String normalizeImportedSheetName(String candidateName, int sheetNumber) {
        if (!StringUtils.hasText(candidateName)) {
            return defaultSheetName(sheetNumber);
        }
        return normalizeRequiredSheetName(candidateName);
    }

    private String nextDefaultSheetName(List<SheetState> sheets) {
        int nextNumber = sheets.size() + 1;
        String candidate = defaultSheetName(nextNumber);
        while (containsSheetName(sheets, candidate)) {
            nextNumber++;
            candidate = defaultSheetName(nextNumber);
        }
        return candidate;
    }

    private boolean containsSheetName(List<SheetState> sheets, String candidate) {
        for (SheetState sheet : sheets) {
            if (sheet.name().equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int findSheetIndex(List<SheetState> sheets, String sheetId) {
        for (int index = 0; index < sheets.size(); index++) {
            if (sheets.get(index).id().equals(sheetId)) {
                return index;
            }
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets sheet not found");
    }

    private String resolveNextActiveSheetId(
            WorkbookState state,
            String removedSheetId,
            List<SheetState> remainingSheets,
            int removedIndex
    ) {
        if (!removedSheetId.equals(state.activeSheetId())) {
            return state.activeSheetId();
        }
        int nextIndex = Math.min(removedIndex, remainingSheets.size() - 1);
        return remainingSheets.get(nextIndex).id();
    }

    private List<SheetState> replaceSheet(List<SheetState> sheets, SheetState replacement) {
        List<SheetState> updatedSheets = new ArrayList<>(sheets.size());
        for (SheetState sheet : sheets) {
            updatedSheets.add(sheet.id().equals(replacement.id()) ? replacement : sheet);
        }
        return updatedSheets;
    }

    private int normalizeFreezeCount(Integer count, int maxAllowed, String fieldName) {
        int safeValue = count == null ? 0 : count;
        if (safeValue < 0 || safeValue > maxAllowed) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is out of range");
        }
        return safeValue;
    }

    private String uniqueSheetId(Set<String> usedIds) {
        String candidateId = generateSheetId();
        while (!usedIds.add(candidateId)) {
            candidateId = generateSheetId();
        }
        return candidateId;
    }

    private String generateSheetId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String defaultSheetName(int sheetNumber) {
        return DEFAULT_SHEET_PREFIX + " " + sheetNumber;
    }

    private int normalizeImportedRowCount(int rowCount) {
        int candidate = rowCount < 1 ? EMPTY_IMPORTED_DIMENSION : rowCount;
        return sheetsGridService.normalizeRowCount(candidate);
    }

    private int normalizeImportedColCount(int colCount) {
        int candidate = colCount < 1 ? EMPTY_IMPORTED_DIMENSION : colCount;
        return sheetsGridService.normalizeColCount(candidate);
    }

    public record WorkbookState(List<SheetState> sheets, String activeSheetId) {
    }

    public record SheetState(
            String id,
            String name,
            int rowCount,
            int colCount,
            int frozenRowCount,
            int frozenColCount,
            List<List<String>> grid
    ) {

        public SheetState withName(String nextName) {
            return new SheetState(id, nextName, rowCount, colCount, frozenRowCount, frozenColCount, grid);
        }

        public SheetState withGrid(List<List<String>> nextGrid) {
            return new SheetState(id, name, rowCount, colCount, frozenRowCount, frozenColCount, nextGrid);
        }

        public SheetState withFreeze(int nextFrozenRowCount, int nextFrozenColCount) {
            return new SheetState(id, name, rowCount, colCount, nextFrozenRowCount, nextFrozenColCount, grid);
        }
    }

    public record SheetSeed(
            String name,
            int rowCount,
            int colCount,
            List<List<String>> grid,
            int frozenRowCount,
            int frozenColCount
    ) {
        public SheetSeed(String name, int rowCount, int colCount, List<List<String>> grid) {
            this(name, rowCount, colCount, grid, 0, 0);
        }
    }

    private record StoredSheet(
            String id,
            String name,
            Integer rowCount,
            Integer colCount,
            Integer frozenRowCount,
            Integer frozenColCount,
            List<List<String>> grid
    ) {
    }
}
