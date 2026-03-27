package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateStandardNoteFolderRequest;
import com.mmmail.server.model.dto.CreateStandardNoteRequest;
import com.mmmail.server.model.dto.ToggleStandardNoteChecklistItemRequest;
import com.mmmail.server.model.dto.UpdateStandardNoteFolderRequest;
import com.mmmail.server.model.dto.UpdateStandardNoteRequest;
import com.mmmail.server.model.vo.StandardNoteDetailVo;
import com.mmmail.server.model.vo.StandardNoteFolderVo;
import com.mmmail.server.model.vo.StandardNoteSummaryVo;
import com.mmmail.server.model.vo.StandardNotesExportVo;
import com.mmmail.server.model.vo.StandardNotesOverviewVo;
import com.mmmail.server.service.StandardNotesChecklistService;
import com.mmmail.server.service.StandardNotesFolderService;
import com.mmmail.server.service.StandardNotesService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/standard-notes")
public class StandardNotesController {

    private final StandardNotesService standardNotesService;
    private final StandardNotesFolderService standardNotesFolderService;
    private final StandardNotesChecklistService standardNotesChecklistService;

    public StandardNotesController(
            StandardNotesService standardNotesService,
            StandardNotesFolderService standardNotesFolderService,
            StandardNotesChecklistService standardNotesChecklistService
    ) {
        this.standardNotesService = standardNotesService;
        this.standardNotesFolderService = standardNotesFolderService;
        this.standardNotesChecklistService = standardNotesChecklistService;
    }

    @GetMapping("/overview")
    public Result<StandardNotesOverviewVo> overview(HttpServletRequest httpRequest) {
        return Result.success(standardNotesService.getOverview(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/folders")
    public Result<List<StandardNoteFolderVo>> listFolders(HttpServletRequest httpRequest) {
        return Result.success(standardNotesFolderService.list(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/folders")
    public Result<StandardNoteFolderVo> createFolder(
            @Valid @RequestBody CreateStandardNoteFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(standardNotesFolderService.create(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/folders/{folderId}")
    public Result<StandardNoteFolderVo> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody UpdateStandardNoteFolderRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(standardNotesFolderService.update(SecurityUtils.currentUserId(), folderId, request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/folders/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId, HttpServletRequest httpRequest) {
        standardNotesFolderService.delete(SecurityUtils.currentUserId(), folderId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/notes")
    public Result<List<StandardNoteSummaryVo>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String noteType,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(standardNotesService.list(SecurityUtils.currentUserId(), keyword, includeArchived, noteType, tag, folderId, limit));
    }

    @PostMapping("/notes")
    public Result<StandardNoteDetailVo> create(
            @Valid @RequestBody CreateStandardNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(standardNotesService.create(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/notes/{noteId}")
    public Result<StandardNoteDetailVo> get(@PathVariable Long noteId) {
        return Result.success(standardNotesService.get(SecurityUtils.currentUserId(), noteId));
    }

    @PutMapping("/notes/{noteId}")
    public Result<StandardNoteDetailVo> update(
            @PathVariable Long noteId,
            @Valid @RequestBody UpdateStandardNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(standardNotesService.update(SecurityUtils.currentUserId(), noteId, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/notes/{noteId}/checklist-items/{itemIndex}/toggle")
    public Result<StandardNoteDetailVo> toggleChecklistItem(
            @PathVariable Long noteId,
            @PathVariable int itemIndex,
            @Valid @RequestBody ToggleStandardNoteChecklistItemRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(standardNotesChecklistService.toggleItem(
                SecurityUtils.currentUserId(),
                noteId,
                itemIndex,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/notes/{noteId}")
    public Result<Void> delete(@PathVariable Long noteId, HttpServletRequest httpRequest) {
        standardNotesService.delete(SecurityUtils.currentUserId(), noteId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/export")
    public Result<StandardNotesExportVo> export(HttpServletRequest httpRequest) {
        return Result.success(standardNotesService.exportWorkspace(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }
}
