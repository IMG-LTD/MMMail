package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.EvaluateSheetsCellsRequest;
import com.mmmail.server.model.vo.SheetsDependencyGraphVo;
import com.mmmail.server.model.vo.SheetsFormulaEvaluationVo;
import com.mmmail.server.model.vo.SheetsWorkbookDetailVo;
import com.mmmail.server.security.RequireEntitlement;
import com.mmmail.server.service.SheetsFormulaApiService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireEntitlement("SHEETS")
@RequestMapping("/api/v1/sheets/{workbookId}")
public class SheetsFormulaController {

    private final SheetsFormulaApiService sheetsFormulaApiService;

    public SheetsFormulaController(SheetsFormulaApiService sheetsFormulaApiService) {
        this.sheetsFormulaApiService = sheetsFormulaApiService;
    }

    @PostMapping("/cells/evaluate")
    public Result<SheetsFormulaEvaluationVo> evaluate(
            @PathVariable Long workbookId,
            @Valid @RequestBody EvaluateSheetsCellsRequest request
    ) {
        return Result.success(sheetsFormulaApiService.evaluate(SecurityUtils.currentUserId(), workbookId, request.cells()));
    }

    @GetMapping("/dependency-graph")
    public Result<SheetsDependencyGraphVo> dependencyGraph(@PathVariable Long workbookId) {
        return Result.success(sheetsFormulaApiService.dependencyGraph(SecurityUtils.currentUserId(), workbookId));
    }

    @PostMapping("/recalculate")
    public Result<SheetsWorkbookDetailVo> recalculate(@PathVariable Long workbookId) {
        return Result.success(sheetsFormulaApiService.recalculate(SecurityUtils.currentUserId(), workbookId));
    }
}
