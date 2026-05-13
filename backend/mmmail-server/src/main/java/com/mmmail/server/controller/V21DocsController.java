package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateDocsNoteCommentRequest;
import com.mmmail.server.model.dto.CreateDocsNoteRequest;
import com.mmmail.server.model.dto.CreateDocsNoteShareRequest;
import com.mmmail.server.model.dto.UpdateDocsNoteRequest;
import com.mmmail.server.model.vo.DocsNoteCommentVo;
import com.mmmail.server.model.vo.DocsNoteDetailVo;
import com.mmmail.server.model.vo.DocsNoteShareVo;
import com.mmmail.server.model.vo.DocsNoteSummaryVo;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.service.DocsCollaborationService;
import com.mmmail.server.service.DocsService;
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
@RequestMapping("/api/v2/docs")
public class V21DocsController {

    private final DocsService docsService;
    private final DocsCollaborationService docsCollaborationService;

    public V21DocsController(DocsService docsService, DocsCollaborationService docsCollaborationService) {
        this.docsService = docsService;
        this.docsCollaborationService = docsCollaborationService;
    }

    @GetMapping
    public Result<List<DocsNoteSummaryVo>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(docsService.list(SecurityUtils.currentUserId(), keyword, limit));
    }

    @PostMapping
    public Result<DocsNoteDetailVo> create(
            @Valid @RequestBody CreateDocsNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsService.create(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                request.title(),
                request.content(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}")
    public Result<DocsNoteDetailVo> get(@PathVariable Long noteId) {
        return Result.success(docsService.get(SecurityUtils.currentUserId(), noteId));
    }

    @PatchMapping("/{noteId}")
    public Result<DocsNoteDetailVo> update(
            @PathVariable Long noteId,
            @Valid @RequestBody UpdateDocsNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsService.update(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                request.title(),
                request.content(),
                request.currentVersion(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}/comments")
    public Result<List<DocsNoteCommentVo>> listComments(@PathVariable Long noteId) {
        return Result.success(docsCollaborationService.listComments(SecurityUtils.currentUserId(), noteId, true));
    }

    @PostMapping("/{noteId}/comments")
    public Result<DocsNoteCommentVo> createComment(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateDocsNoteCommentRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.createComment(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{noteId}/share")
    public Result<DocsNoteShareVo> share(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateDocsNoteShareRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.createShare(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
