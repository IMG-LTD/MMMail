package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateDocsNoteCommentRequest;
import com.mmmail.server.model.dto.CreateDocsNoteRequest;
import com.mmmail.server.model.dto.CreateDocsNoteShareRequest;
import com.mmmail.server.model.dto.CreateDocsNoteSuggestionRequest;
import com.mmmail.server.model.dto.HeartbeatDocsNotePresenceRequest;
import com.mmmail.server.model.dto.ResolveDocsNoteSuggestionRequest;
import com.mmmail.server.model.dto.UpdateDocsNoteShareRequest;
import com.mmmail.server.model.dto.UpdateDocsNoteRequest;
import com.mmmail.server.model.vo.DocsNoteCollaborationOverviewVo;
import com.mmmail.server.model.vo.DocsNoteCommentVo;
import com.mmmail.server.model.vo.DocsNoteDetailVo;
import com.mmmail.server.model.vo.DocsNotePresenceVo;
import com.mmmail.server.model.vo.DocsNoteShareVo;
import com.mmmail.server.model.vo.DocsNoteSuggestionVo;
import com.mmmail.server.model.vo.DocsNoteSummaryVo;
import com.mmmail.server.model.vo.DocsNoteSyncVo;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.service.DocsCollaborationService;
import com.mmmail.server.service.DocsService;
import com.mmmail.server.service.DocsSuggestionService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docs/notes")
public class DocsController {

    private final DocsService docsService;
    private final DocsCollaborationService docsCollaborationService;
    private final DocsSuggestionService docsSuggestionService;

    public DocsController(
            DocsService docsService,
            DocsCollaborationService docsCollaborationService,
            DocsSuggestionService docsSuggestionService
    ) {
        this.docsService = docsService;
        this.docsCollaborationService = docsCollaborationService;
        this.docsSuggestionService = docsSuggestionService;
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

    @PutMapping("/{noteId}")
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

    @DeleteMapping("/{noteId}")
    public Result<Void> delete(@PathVariable Long noteId, HttpServletRequest httpRequest) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        docsService.delete(principal.userId(), principal.email(), principal.sessionId(), noteId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{noteId}/collaboration")
    public Result<DocsNoteCollaborationOverviewVo> getCollaboration(@PathVariable Long noteId) {
        return Result.success(docsCollaborationService.getOverview(SecurityUtils.currentUserId(), noteId));
    }

    @GetMapping("/{noteId}/shares")
    public Result<List<DocsNoteShareVo>> listShares(@PathVariable Long noteId) {
        return Result.success(docsCollaborationService.listShares(SecurityUtils.currentUserId(), noteId));
    }

    @PostMapping("/{noteId}/shares")
    public Result<DocsNoteShareVo> createShare(
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

    @PutMapping("/{noteId}/shares/{shareId}")
    public Result<DocsNoteShareVo> updateSharePermission(
            @PathVariable Long noteId,
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateDocsNoteShareRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.updateSharePermission(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/{noteId}/shares/{shareId}")
    public Result<DocsNoteShareVo> removeShare(
            @PathVariable Long noteId,
            @PathVariable Long shareId,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.removeShare(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                shareId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}/comments")
    public Result<List<DocsNoteCommentVo>> listComments(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "true") boolean includeResolved
    ) {
        return Result.success(docsCollaborationService.listComments(SecurityUtils.currentUserId(), noteId, includeResolved));
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

    @PostMapping("/{noteId}/comments/{commentId}/resolve")
    public Result<DocsNoteCommentVo> resolveComment(
            @PathVariable Long noteId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.resolveComment(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                commentId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}/suggestions")
    public Result<List<DocsNoteSuggestionVo>> listSuggestions(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "true") boolean includeResolved
    ) {
        return Result.success(docsSuggestionService.listSuggestions(SecurityUtils.currentUserId(), noteId, includeResolved));
    }

    @PostMapping("/{noteId}/suggestions")
    public Result<DocsNoteSuggestionVo> createSuggestion(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateDocsNoteSuggestionRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsSuggestionService.createSuggestion(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{noteId}/suggestions/{suggestionId}/accept")
    public Result<DocsNoteSuggestionVo> acceptSuggestion(
            @PathVariable Long noteId,
            @PathVariable Long suggestionId,
            @Valid @RequestBody ResolveDocsNoteSuggestionRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsSuggestionService.acceptSuggestion(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                suggestionId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{noteId}/suggestions/{suggestionId}/reject")
    public Result<DocsNoteSuggestionVo> rejectSuggestion(
            @PathVariable Long noteId,
            @PathVariable Long suggestionId,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsSuggestionService.rejectSuggestion(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                suggestionId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}/presence")
    public Result<List<DocsNotePresenceVo>> listPresence(@PathVariable Long noteId) {
        return Result.success(docsCollaborationService.listPresence(SecurityUtils.currentUserId(), noteId));
    }

    @PostMapping("/{noteId}/presence/heartbeat")
    public Result<DocsNotePresenceVo> heartbeat(
            @PathVariable Long noteId,
            @Valid @RequestBody HeartbeatDocsNotePresenceRequest request,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        return Result.success(docsCollaborationService.heartbeat(
                principal.userId(),
                principal.email(),
                principal.sessionId(),
                noteId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/{noteId}/sync")
    public Result<DocsNoteSyncVo> getSync(
            @PathVariable Long noteId,
            @RequestParam(required = false) Long afterEventId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(docsCollaborationService.getSync(SecurityUtils.currentUserId(), noteId, afterEventId, limit));
    }

    @GetMapping(value = "/{noteId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSync(
            @PathVariable Long noteId,
            @RequestParam(required = false) Long afterEventId
    ) {
        return docsCollaborationService.openSyncStream(SecurityUtils.currentUserId(), noteId, afterEventId);
    }
}
