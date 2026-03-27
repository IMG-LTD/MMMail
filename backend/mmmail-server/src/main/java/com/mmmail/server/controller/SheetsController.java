package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateSheetsWorkbookRequest;
import com.mmmail.server.model.dto.CreateSheetsWorkbookShareRequest;
import com.mmmail.server.model.dto.CreateSheetsWorkbookSheetRequest;
import com.mmmail.server.model.dto.FreezeSheetsWorkbookSheetRequest;
import com.mmmail.server.model.dto.RenameSheetsWorkbookRequest;
import com.mmmail.server.model.dto.RenameSheetsWorkbookSheetRequest;
import com.mmmail.server.model.dto.RespondSheetsWorkbookShareRequest;
import com.mmmail.server.model.dto.SetActiveSheetsWorkbookSheetRequest;
import com.mmmail.server.model.dto.SortSheetsWorkbookSheetRequest;
import com.mmmail.server.model.dto.UpdateSheetsWorkbookCellsRequest;
import com.mmmail.server.model.dto.UpdateSheetsWorkbookShareRequest;
import com.mmmail.server.model.vo.SheetsIncomingShareVo;
import com.mmmail.server.model.vo.SheetsWorkbookDetailVo;
import com.mmmail.server.model.vo.SheetsWorkbookExportVo;
import com.mmmail.server.model.vo.SheetsWorkbookShareVo;
import com.mmmail.server.model.vo.SheetsWorkbookSummaryVo;
import com.mmmail.server.model.vo.SheetsWorkbookVersionVo;
import com.mmmail.server.service.SheetsService;
import com.mmmail.server.service.SheetsSharingService;
import com.mmmail.server.service.SheetsVersionService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sheets/workbooks")
public class SheetsController {

    private final SheetsService sheetsService;
    private final SheetsSharingService sheetsSharingService;
    private final SheetsVersionService sheetsVersionService;

    public SheetsController(
            SheetsService sheetsService,
            SheetsSharingService sheetsSharingService,
            SheetsVersionService sheetsVersionService
    ) {
        this.sheetsService = sheetsService;
        this.sheetsSharingService = sheetsSharingService;
        this.sheetsVersionService = sheetsVersionService;
    }

    @GetMapping
    public Result<List<SheetsWorkbookSummaryVo>> list(@RequestParam(required = false) Integer limit) {
        return Result.success(sheetsService.list(SecurityUtils.currentUserId(), limit));
    }

    @PostMapping
    public Result<SheetsWorkbookDetailVo> create(
            @Valid @RequestBody CreateSheetsWorkbookRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.create(
                SecurityUtils.currentUserId(),
                request.title(),
                request.rowCount(),
                request.colCount(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SheetsWorkbookDetailVo> importWorkbook(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.importWorkbook(
                SecurityUtils.currentUserId(),
                file,
                title,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{workbookId}")
    public Result<SheetsWorkbookDetailVo> get(@PathVariable Long workbookId) {
        return Result.success(sheetsService.get(SecurityUtils.currentUserId(), workbookId));
    }

    @PostMapping("/{workbookId}/shares")
    public Result<SheetsWorkbookShareVo> createShare(
            @PathVariable Long workbookId,
            @Valid @RequestBody CreateSheetsWorkbookShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsSharingService.createShare(
                SecurityUtils.currentUserId(),
                workbookId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{workbookId}/shares")
    public Result<List<SheetsWorkbookShareVo>> listShares(@PathVariable Long workbookId) {
        return Result.success(sheetsSharingService.listShares(SecurityUtils.currentUserId(), workbookId));
    }

    @PutMapping("/{workbookId}/shares/{shareId}")
    public Result<SheetsWorkbookShareVo> updateShare(
            @PathVariable Long workbookId,
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateSheetsWorkbookShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsSharingService.updateSharePermission(
                SecurityUtils.currentUserId(),
                workbookId,
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{workbookId}/shares/{shareId}")
    public Result<Void> removeShare(
            @PathVariable Long workbookId,
            @PathVariable Long shareId,
            HttpServletRequest httpRequest
    ) {
        sheetsSharingService.removeShare(SecurityUtils.currentUserId(), workbookId, shareId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/incoming-shares")
    public Result<List<SheetsIncomingShareVo>> incomingShares(HttpServletRequest httpRequest) {
        return Result.success(sheetsSharingService.listIncomingShares(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/incoming-shares/{shareId}/respond")
    public Result<SheetsIncomingShareVo> respondShare(
            @PathVariable Long shareId,
            @Valid @RequestBody RespondSheetsWorkbookShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsSharingService.respondShare(
                SecurityUtils.currentUserId(),
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{workbookId}/versions")
    public Result<List<SheetsWorkbookVersionVo>> listVersions(@PathVariable Long workbookId) {
        return Result.success(sheetsVersionService.listVersions(SecurityUtils.currentUserId(), workbookId));
    }

    @PostMapping("/{workbookId}/versions/{versionId}/restore")
    public Result<SheetsWorkbookDetailVo> restoreVersion(
            @PathVariable Long workbookId,
            @PathVariable Long versionId,
            HttpServletRequest httpRequest
    ) {
        sheetsVersionService.restoreVersion(
                SecurityUtils.currentUserId(),
                workbookId,
                versionId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(sheetsService.get(SecurityUtils.currentUserId(), workbookId));
    }

    @GetMapping("/{workbookId}/export")
    public Result<SheetsWorkbookExportVo> exportWorkbook(
            @PathVariable Long workbookId,
            @RequestParam String format,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.exportWorkbook(
                SecurityUtils.currentUserId(),
                workbookId,
                format,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/rename")
    public Result<SheetsWorkbookDetailVo> rename(
            @PathVariable Long workbookId,
            @Valid @RequestBody RenameSheetsWorkbookRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.rename(
                SecurityUtils.currentUserId(),
                workbookId,
                request.title(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{workbookId}/sheets")
    public Result<SheetsWorkbookDetailVo> createSheet(
            @PathVariable Long workbookId,
            @Valid @RequestBody CreateSheetsWorkbookSheetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.createSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                request.name(),
                request.rowCount(),
                request.colCount(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/sheets/{sheetId}/rename")
    public Result<SheetsWorkbookDetailVo> renameSheet(
            @PathVariable Long workbookId,
            @PathVariable String sheetId,
            @Valid @RequestBody RenameSheetsWorkbookSheetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.renameSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                sheetId,
                request.name(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{workbookId}/sheets/{sheetId}")
    public Result<SheetsWorkbookDetailVo> deleteSheet(
            @PathVariable Long workbookId,
            @PathVariable String sheetId,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.deleteSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                sheetId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/active-sheet")
    public Result<SheetsWorkbookDetailVo> setActiveSheet(
            @PathVariable Long workbookId,
            @Valid @RequestBody SetActiveSheetsWorkbookSheetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.setActiveSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                request.sheetId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/sheets/{sheetId}/sort")
    public Result<SheetsWorkbookDetailVo> sortSheet(
            @PathVariable Long workbookId,
            @PathVariable String sheetId,
            @Valid @RequestBody SortSheetsWorkbookSheetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.sortSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                sheetId,
                request.currentVersion(),
                request.columnIndex(),
                request.direction(),
                request.includeHeader(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/sheets/{sheetId}/freeze")
    public Result<SheetsWorkbookDetailVo> freezeSheet(
            @PathVariable Long workbookId,
            @PathVariable String sheetId,
            @Valid @RequestBody FreezeSheetsWorkbookSheetRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.freezeSheet(
                SecurityUtils.currentUserId(),
                workbookId,
                sheetId,
                request.currentVersion(),
                request.frozenRowCount(),
                request.frozenColCount(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PutMapping("/{workbookId}/cells")
    public Result<SheetsWorkbookDetailVo> updateCells(
            @PathVariable Long workbookId,
            @Valid @RequestBody UpdateSheetsWorkbookCellsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(sheetsService.updateCells(
                SecurityUtils.currentUserId(),
                workbookId,
                request.currentVersion(),
                request.sheetId(),
                request.edits(),
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{workbookId}")
    public Result<Void> delete(@PathVariable Long workbookId, HttpServletRequest httpRequest) {
        sheetsService.delete(SecurityUtils.currentUserId(), workbookId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
