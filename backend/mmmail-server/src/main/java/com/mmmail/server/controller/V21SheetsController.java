package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateSheetsWorkbookRequest;
import com.mmmail.server.model.dto.UpdateSheetsWorkbookCellsRequest;
import com.mmmail.server.model.vo.SheetsWorkbookDetailVo;
import com.mmmail.server.model.vo.SheetsWorkbookSummaryVo;
import com.mmmail.server.service.SheetsService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v2/sheets")
public class V21SheetsController {

    private final SheetsService sheetsService;

    public V21SheetsController(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
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

    @GetMapping("/{workbookId}")
    public Result<SheetsWorkbookDetailVo> get(@PathVariable Long workbookId) {
        return Result.success(sheetsService.get(SecurityUtils.currentUserId(), workbookId));
    }

    @PatchMapping("/{workbookId}")
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

    @PostMapping("/{workbookId}/imports")
    public Result<SheetsWorkbookDetailVo> importWorkbook(@PathVariable Long workbookId) {
        throw new BizException(
                ErrorCode.INVALID_ARGUMENT,
                "Sheets JSON import is not supported by the v2.1 runtime bridge"
        );
    }
}
