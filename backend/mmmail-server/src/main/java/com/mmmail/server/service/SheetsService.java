package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.model.dto.SheetCellEditInput;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SheetsWorkbookDetailVo;
import com.mmmail.server.model.vo.SheetsWorkbookExportVo;
import com.mmmail.server.model.vo.SheetsWorkbookSheetVo;
import com.mmmail.server.model.vo.SheetsWorkbookSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SheetsService {

    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final SheetsGridService sheetsGridService;
    private final SheetsFormulaService sheetsFormulaService;
    private final SheetsImportExportService sheetsImportExportService;
    private final SheetsWorkbookStateService sheetsWorkbookStateService;
    private final SheetsSortService sheetsSortService;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;
    private final SheetsAccessService sheetsAccessService;
    private final SheetsSharingService sheetsSharingService;
    private final SheetsVersionService sheetsVersionService;

    public SheetsService(
            SheetsWorkbookMapper sheetsWorkbookMapper,
            SheetsGridService sheetsGridService,
            SheetsFormulaService sheetsFormulaService,
            SheetsImportExportService sheetsImportExportService,
            SheetsWorkbookStateService sheetsWorkbookStateService,
            SheetsSortService sheetsSortService,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService,
            SheetsAccessService sheetsAccessService,
            SheetsSharingService sheetsSharingService,
            SheetsVersionService sheetsVersionService
    ) {
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.sheetsGridService = sheetsGridService;
        this.sheetsFormulaService = sheetsFormulaService;
        this.sheetsImportExportService = sheetsImportExportService;
        this.sheetsWorkbookStateService = sheetsWorkbookStateService;
        this.sheetsSortService = sheetsSortService;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
        this.sheetsAccessService = sheetsAccessService;
        this.sheetsSharingService = sheetsSharingService;
        this.sheetsVersionService = sheetsVersionService;
    }

    public List<SheetsWorkbookSummaryVo> list(Long userId, Integer limit) {
        return sheetsAccessService.listVisibleWorkbooks(userId, limit).stream()
                .map(this::toSummaryVo)
                .toList();
    }

    @Transactional
    public SheetsWorkbookDetailVo create(Long userId, String title, Integer rowCount, Integer colCount, String ipAddress) {
        int safeRowCount = sheetsGridService.normalizeRowCount(rowCount);
        int safeColCount = sheetsGridService.normalizeColCount(colCount);
        SheetsWorkbook workbook = buildWorkbook(
                userId,
                normalizeTitle(title),
                sheetsWorkbookStateService.createInitialState(safeRowCount, safeColCount)
        );
        sheetsWorkbookMapper.insert(workbook);
        sheetsVersionService.recordSnapshot(workbook, userId, SheetsVersionService.SOURCE_CREATE);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_CREATE", workbook, ipAddress, null);
        return get(userId, workbook.getId());
    }

    @Transactional
    public SheetsWorkbookDetailVo importWorkbook(Long userId, MultipartFile file, String title, String ipAddress) {
        SheetsImportExportService.ImportedWorkbook imported = sheetsImportExportService.importWorkbook(file, title);
        SheetsWorkbookStateService.WorkbookState state = buildImportedState(imported);
        SheetsWorkbook workbook = buildWorkbook(userId, normalizeTitle(imported.title()), state);
        sheetsWorkbookMapper.insert(workbook);
        sheetsVersionService.recordSnapshot(workbook, userId, SheetsVersionService.SOURCE_IMPORT);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_IMPORT", workbook, ipAddress, "format=" + imported.sourceFormat());
        return get(userId, workbook.getId());
    }

    @Transactional
    public SheetsWorkbookDetailVo get(Long userId, Long workbookId) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireAccessible(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        touchWorkbook(workbook);
        sheetsWorkbookMapper.updateById(workbook);
        return toDetailVo(workbook, sheetsWorkbookStateService.readState(workbook), context);
    }

    @Transactional
    public SheetsWorkbookDetailVo rename(Long userId, Long workbookId, String title, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        workbook.setTitle(normalizeTitle(title));
        applyMutationMetadata(workbook);
        sheetsWorkbookMapper.updateById(workbook);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_RENAME", workbook, ipAddress, null);
        return toDetailVo(workbook, sheetsWorkbookStateService.readState(workbook), context);
    }

    @Transactional
    public SheetsWorkbookDetailVo createSheet(
            Long userId,
            Long workbookId,
            String name,
            Integer rowCount,
            Integer colCount,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.addSheet(state, name, rowCount, colCount);
        persistWorkbookMutation(workbook, nextState);
        String detail = buildSheetDetail(nextState.activeSheetId(), sheetsWorkbookStateService.activeSheet(nextState).name());
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_SHEET_CREATE", workbook, ipAddress, detail);
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo renameSheet(
            Long userId,
            Long workbookId,
            String sheetId,
            String name,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.renameSheet(state, sheetId, name);
        SheetsWorkbookStateService.SheetState renamedSheet = sheetsWorkbookStateService.requireSheet(nextState, sheetId);
        persistWorkbookMutation(workbook, nextState);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_SHEET_RENAME", workbook, ipAddress, buildSheetDetail(sheetId, renamedSheet.name()));
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo deleteSheet(Long userId, Long workbookId, String sheetId, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.SheetState deletedSheet = sheetsWorkbookStateService.requireSheet(state, sheetId);
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.deleteSheet(state, sheetId);
        persistWorkbookMutation(workbook, nextState);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_SHEET_DELETE", workbook, ipAddress, buildSheetDetail(sheetId, deletedSheet.name()));
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo setActiveSheet(Long userId, Long workbookId, String sheetId, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireAccessible(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.setActiveSheet(state, sheetId);
        persistWorkbookState(workbook, nextState);
        SheetsWorkbookStateService.SheetState activeSheet = sheetsWorkbookStateService.activeSheet(nextState);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_ACTIVE_SHEET_SET", workbook, ipAddress, buildSheetDetail(activeSheet.id(), activeSheet.name()));
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo sortSheet(
            Long userId,
            Long workbookId,
            String sheetId,
            Integer currentVersion,
            Integer columnIndex,
            String direction,
            Boolean includeHeader,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        validateCurrentVersion(workbook, currentVersion);
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.SheetState targetSheet = sheetsWorkbookStateService.requireSheet(state, sheetId);
        SheetsFormulaService.ComputationResult computation = sheetsFormulaService.compute(
                targetSheet.grid(),
                targetSheet.rowCount(),
                targetSheet.colCount()
        );
        List<List<String>> sortedGrid = sheetsSortService.sort(new SheetsSortService.SortSheetRequest(
                targetSheet.grid(),
                computation.computedGrid(),
                targetSheet.rowCount(),
                targetSheet.colCount(),
                columnIndex,
                direction,
                Boolean.TRUE.equals(includeHeader)
        ));
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.updateSheetGrid(state, sheetId, sortedGrid);
        persistWorkbookMutation(workbook, nextState);
        publishSheetsEvent(
                userId,
                "SHEETS_WORKBOOK_SHEET_SORT",
                workbook,
                ipAddress,
                buildSheetDetail(sheetId, targetSheet.name())
                        + ",columnIndex=" + columnIndex
                        + ",direction=" + direction
                        + ",includeHeader=" + Boolean.TRUE.equals(includeHeader)
        );
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo freezeSheet(
            Long userId,
            Long workbookId,
            String sheetId,
            Integer currentVersion,
            Integer frozenRowCount,
            Integer frozenColCount,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        validateCurrentVersion(workbook, currentVersion);
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookStateService.SheetState targetSheet = sheetsWorkbookStateService.requireSheet(state, sheetId);
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.updateSheetFreeze(
                state,
                sheetId,
                frozenRowCount,
                frozenColCount
        );
        persistWorkbookMutation(workbook, nextState);
        publishSheetsEvent(
                userId,
                "SHEETS_WORKBOOK_SHEET_FREEZE",
                workbook,
                ipAddress,
                buildSheetDetail(sheetId, targetSheet.name())
                        + ",frozenRowCount=" + frozenRowCount
                        + ",frozenColCount=" + frozenColCount
        );
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookDetailVo updateCells(
            Long userId,
            Long workbookId,
            Integer currentVersion,
            String sheetId,
            List<SheetCellEditInput> edits,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireEditable(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        validateCurrentVersion(workbook, currentVersion);
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        String targetSheetId = sheetsWorkbookStateService.resolveSheetId(state, sheetId);
        SheetsWorkbookStateService.SheetState targetSheet = sheetsWorkbookStateService.requireSheet(state, targetSheetId);
        List<List<String>> updatedGrid = sheetsGridService.applyEdits(
                targetSheet.grid(),
                targetSheet.rowCount(),
                targetSheet.colCount(),
                edits
        );
        SheetsWorkbookStateService.WorkbookState nextState = sheetsWorkbookStateService.updateSheetGrid(state, targetSheetId, updatedGrid);
        persistWorkbookMutation(workbook, nextState);
        sheetsVersionService.recordSnapshot(workbook, userId, SheetsVersionService.SOURCE_UPDATE_CELLS);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_UPDATE_CELLS", workbook, ipAddress, buildSheetDetail(targetSheetId, targetSheet.name()));
        return toDetailVo(workbook, nextState, context);
    }

    @Transactional
    public SheetsWorkbookExportVo exportWorkbook(Long userId, Long workbookId, String format, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireAccessible(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        List<SheetsWorkbookSheetVo> sheets = buildSheetViews(state);
        SheetsWorkbookExportVo export = sheetsImportExportService.exportWorkbook(workbook, sheets, state.activeSheetId(), format);
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_EXPORT", workbook, ipAddress, "format=" + export.format());
        return export;
    }

    @Transactional
    public void delete(Long userId, Long workbookId, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        sheetsSharingService.purgeByWorkbookId(workbookId);
        sheetsVersionService.purgeByWorkbookId(workbookId);
        sheetsWorkbookMapper.deleteById(workbook.getId());
        publishSheetsEvent(userId, "SHEETS_WORKBOOK_DELETE", workbook, ipAddress, null);
    }

    private SheetsWorkbook buildWorkbook(
            Long userId,
            String title,
            SheetsWorkbookStateService.WorkbookState state
    ) {
        LocalDateTime now = LocalDateTime.now();
        SheetsWorkbook workbook = new SheetsWorkbook();
        workbook.setOwnerId(userId);
        workbook.setTitle(title);
        workbook.setCurrentVersion(1);
        workbook.setLastOpenedAt(now);
        workbook.setCreatedAt(now);
        workbook.setUpdatedAt(now);
        workbook.setDeleted(0);
        sheetsWorkbookStateService.syncWorkbook(workbook, state);
        return workbook;
    }

    private SheetsWorkbookStateService.WorkbookState buildImportedState(SheetsImportExportService.ImportedWorkbook imported) {
        List<SheetsWorkbookStateService.SheetSeed> seeds = new ArrayList<>(imported.sheets().size());
        for (SheetsImportExportService.ImportedSheet sheet : imported.sheets()) {
            seeds.add(new SheetsWorkbookStateService.SheetSeed(sheet.name(), sheet.rowCount(), sheet.colCount(), sheet.grid()));
        }
        return sheetsWorkbookStateService.createImportedState(seeds);
    }

    private void persistWorkbookMutation(
            SheetsWorkbook workbook,
            SheetsWorkbookStateService.WorkbookState state
    ) {
        sheetsWorkbookStateService.syncWorkbook(workbook, state);
        applyMutationMetadata(workbook);
        sheetsWorkbookMapper.updateById(workbook);
    }

    private void persistWorkbookState(
            SheetsWorkbook workbook,
            SheetsWorkbookStateService.WorkbookState state
    ) {
        sheetsWorkbookStateService.syncWorkbook(workbook, state);
        applyStateMetadata(workbook);
        sheetsWorkbookMapper.updateById(workbook);
    }

    private void touchWorkbook(SheetsWorkbook workbook) {
        workbook.setLastOpenedAt(LocalDateTime.now());
    }

    private void validateCurrentVersion(SheetsWorkbook workbook, Integer currentVersion) {
        if (currentVersion == null || currentVersion < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "currentVersion is required");
        }
        if (!currentVersion.equals(workbook.getCurrentVersion())) {
            throw new BizException(
                    ErrorCode.SHEETS_WORKBOOK_VERSION_CONFLICT,
                    "Sheets workbook has been updated by another session"
            );
        }
    }

    private void applyMutationMetadata(SheetsWorkbook workbook) {
        LocalDateTime now = LocalDateTime.now();
        workbook.setCurrentVersion(workbook.getCurrentVersion() + 1);
        workbook.setUpdatedAt(now);
        workbook.setLastOpenedAt(now);
    }

    private void applyStateMetadata(SheetsWorkbook workbook) {
        LocalDateTime now = LocalDateTime.now();
        workbook.setUpdatedAt(now);
        workbook.setLastOpenedAt(now);
    }

    private void publishSheetsEvent(
            Long userId,
            String eventType,
            SheetsWorkbook workbook,
            String ipAddress,
            String extraDetail
    ) {
        String detail = buildAuditDetail(workbook);
        if (StringUtils.hasText(extraDetail)) {
            detail += "," + extraDetail.trim();
        }
        AuditEventVo event = auditService.recordEvent(userId, eventType, detail, ipAddress);
        suiteCollaborationService.publishToUser(userId, event);
        if (!workbook.getOwnerId().equals(userId)) {
            suiteCollaborationService.publishToUser(workbook.getOwnerId(), event);
        }
    }

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets title is required");
        }
        return title.trim();
    }

    private SheetsWorkbookSummaryVo toSummaryVo(SheetsAccessService.SheetsWorkbookAccessContext context) {
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        SheetsWorkbookSheetVo activeSheet = buildSheetView(sheetsWorkbookStateService.activeSheet(state));
        return new SheetsWorkbookSummaryVo(
                String.valueOf(workbook.getId()),
                workbook.getTitle(),
                activeSheet.rowCount(),
                activeSheet.colCount(),
                activeSheet.filledCellCount(),
                activeSheet.formulaCellCount(),
                activeSheet.computedErrorCount(),
                workbook.getCurrentVersion(),
                state.sheets().size(),
                state.activeSheetId(),
                workbook.getUpdatedAt(),
                workbook.getLastOpenedAt(),
                context.permission(),
                context.scope(),
                context.owner() == null ? "" : context.owner().getEmail(),
                context.owner() == null ? "" : context.owner().getDisplayName(),
                context.collaboratorCount(),
                context.canEdit()
        );
    }

    private SheetsWorkbookDetailVo toDetailVo(
            SheetsWorkbook workbook,
            SheetsWorkbookStateService.WorkbookState state,
            SheetsAccessService.SheetsWorkbookAccessContext context
    ) {
        List<SheetsWorkbookSheetVo> sheets = buildSheetViews(state);
        SheetsWorkbookSheetVo activeSheet = requireSheetView(sheets, state.activeSheetId());
        return new SheetsWorkbookDetailVo(
                String.valueOf(workbook.getId()),
                workbook.getTitle(),
                activeSheet.rowCount(),
                activeSheet.colCount(),
                activeSheet.filledCellCount(),
                activeSheet.formulaCellCount(),
                activeSheet.computedErrorCount(),
                workbook.getCurrentVersion(),
                sheets.size(),
                state.activeSheetId(),
                sheets,
                activeSheet.grid(),
                activeSheet.computedGrid(),
                SheetsImportExportService.SUPPORTED_IMPORT_FORMATS,
                SheetsImportExportService.SUPPORTED_EXPORT_FORMATS,
                workbook.getCreatedAt(),
                workbook.getUpdatedAt(),
                workbook.getLastOpenedAt(),
                context.permission(),
                context.scope(),
                context.owner() == null ? "" : context.owner().getEmail(),
                context.owner() == null ? "" : context.owner().getDisplayName(),
                context.collaboratorCount(),
                context.canEdit(),
                context.isOwner(),
                context.isOwner()
        );
    }

    private List<SheetsWorkbookSheetVo> buildSheetViews(SheetsWorkbookStateService.WorkbookState state) {
        return state.sheets().stream().map(this::buildSheetView).toList();
    }

    private SheetsWorkbookSheetVo buildSheetView(SheetsWorkbookStateService.SheetState sheet) {
        SheetsFormulaService.ComputationResult computation = sheetsFormulaService.compute(
                sheet.grid(),
                sheet.rowCount(),
                sheet.colCount()
        );
        return new SheetsWorkbookSheetVo(
                sheet.id(),
                sheet.name(),
                sheet.rowCount(),
                sheet.colCount(),
                sheet.frozenRowCount(),
                sheet.frozenColCount(),
                sheetsGridService.countFilledCells(sheet.grid()),
                computation.formulaCellCount(),
                computation.computedErrorCount(),
                sheet.grid(),
                computation.computedGrid()
        );
    }

    private SheetsWorkbookSheetVo requireSheetView(List<SheetsWorkbookSheetVo> sheets, String sheetId) {
        for (SheetsWorkbookSheetVo sheet : sheets) {
            if (sheet.id().equals(sheetId)) {
                return sheet;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Sheets active sheet view is missing");
    }

    private String buildSheetDetail(String sheetId, String sheetName) {
        return "sheetId=" + sheetId + ",sheetName=" + sheetName;
    }

    private String buildAuditDetail(SheetsWorkbook workbook) {
        return "workbookId=" + workbook.getId()
                + ",title=" + workbook.getTitle()
                + ",version=" + workbook.getCurrentVersion()
                + ",activeSheetId=" + workbook.getActiveSheetId();
    }
}
