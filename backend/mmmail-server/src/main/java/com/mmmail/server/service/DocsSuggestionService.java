package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DocsNoteSuggestionMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateDocsNoteSuggestionRequest;
import com.mmmail.server.model.dto.ResolveDocsNoteSuggestionRequest;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.DocsNoteSuggestion;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DocsNoteSuggestionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocsSuggestionService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final DocsAccessService docsAccessService;
    private final DocsNoteMapper docsNoteMapper;
    private final DocsNoteSuggestionMapper docsNoteSuggestionMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final DocsCollaborationSyncService docsCollaborationSyncService;
    private final SuiteCollaborationService suiteCollaborationService;

    public DocsSuggestionService(
            DocsAccessService docsAccessService,
            DocsNoteMapper docsNoteMapper,
            DocsNoteSuggestionMapper docsNoteSuggestionMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            DocsCollaborationSyncService docsCollaborationSyncService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.docsAccessService = docsAccessService;
        this.docsNoteMapper = docsNoteMapper;
        this.docsNoteSuggestionMapper = docsNoteSuggestionMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.docsCollaborationSyncService = docsCollaborationSyncService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    public List<DocsNoteSuggestionVo> listSuggestions(Long userId, Long noteId, boolean includeResolved) {
        docsAccessService.requireAccessible(userId, noteId);
        LambdaQueryWrapper<DocsNoteSuggestion> query = new LambdaQueryWrapper<DocsNoteSuggestion>()
                .eq(DocsNoteSuggestion::getNoteId, noteId)
                .orderByDesc(DocsNoteSuggestion::getCreatedAt);
        if (!includeResolved) {
            query.eq(DocsNoteSuggestion::getStatus, STATUS_PENDING);
        }
        return toSuggestionVos(docsNoteSuggestionMapper.selectList(query));
    }

    @Transactional
    public DocsNoteSuggestionVo createSuggestion(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            CreateDocsNoteSuggestionRequest request,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireEditable(userId, noteId);
        validateCreateRequest(context.note(), request);
        DocsNoteSuggestion suggestion = insertSuggestion(userId, noteId, request);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SUGGEST_ADD",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of(
                        "suggestionId=" + suggestion.getId(),
                        "baseVersion=" + suggestion.getBaseVersion()
                )),
                ipAddress
        );
        publish(noteId, event);
        return toSuggestionVos(List.of(suggestion)).getFirst();
    }

    @Transactional
    public DocsNoteSuggestionVo acceptSuggestion(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            Long suggestionId,
            ResolveDocsNoteSuggestionRequest request,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireOwned(userId, noteId);
        DocsNoteSuggestion suggestion = requirePendingSuggestion(noteId, suggestionId);
        DocsNote note = docsNoteMapper.selectById(context.note().getId());
        validateAcceptRequest(note, suggestion, request);
        applySuggestion(note, suggestion);
        resolveSuggestion(suggestion, userId, STATUS_ACCEPTED);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SUGGEST_ACCEPT",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of(
                        "suggestionId=" + suggestion.getId(),
                        "version=" + note.getCurrentVersion()
                )),
                ipAddress
        );
        publish(noteId, event);
        return toSuggestionVos(List.of(suggestion)).getFirst();
    }

    @Transactional
    public DocsNoteSuggestionVo rejectSuggestion(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            Long suggestionId,
            String ipAddress
    ) {
        docsAccessService.requireOwned(userId, noteId);
        DocsNoteSuggestion suggestion = requirePendingSuggestion(noteId, suggestionId);
        resolveSuggestion(suggestion, userId, STATUS_REJECTED);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SUGGEST_REJECT",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of("suggestionId=" + suggestion.getId())),
                ipAddress
        );
        publish(noteId, event);
        return toSuggestionVos(List.of(suggestion)).getFirst();
    }

    private void validateCreateRequest(DocsNote note, CreateDocsNoteSuggestionRequest request) {
        if (request.selectionStart() == null || request.selectionEnd() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Suggestion selection is required");
        }
        if (request.selectionEnd() <= request.selectionStart()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Suggestion selection range is invalid");
        }
        if (request.baseVersion() == null || !request.baseVersion().equals(note.getCurrentVersion())) {
            throw new BizException(ErrorCode.DOCS_NOTE_VERSION_CONFLICT, "Docs note has been updated before suggesting");
        }
        String noteContent = note.getContent() == null ? "" : note.getContent();
        if (request.selectionEnd() > noteContent.length()) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion selection exceeds current content");
        }
        String originalText = noteContent.substring(request.selectionStart(), request.selectionEnd());
        if (!originalText.equals(request.originalText())) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion selection no longer matches the current content");
        }
    }

    private void validateAcceptRequest(
            DocsNote note,
            DocsNoteSuggestion suggestion,
            ResolveDocsNoteSuggestionRequest request
    ) {
        if (request == null || request.currentVersion() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "currentVersion is required");
        }
        if (!request.currentVersion().equals(note.getCurrentVersion())) {
            throw new BizException(ErrorCode.DOCS_NOTE_VERSION_CONFLICT, "Docs note has been updated by another collaborator");
        }
        if (!suggestion.getBaseVersion().equals(note.getCurrentVersion())) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion is based on an older document version");
        }
        String content = note.getContent() == null ? "" : note.getContent();
        if (suggestion.getSelectionEnd() > content.length()) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion selection exceeds current content");
        }
        String selected = content.substring(suggestion.getSelectionStart(), suggestion.getSelectionEnd());
        if (!selected.equals(suggestion.getOriginalText())) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion selection no longer matches the current content");
        }
    }

    private DocsNoteSuggestion insertSuggestion(Long userId, Long noteId, CreateDocsNoteSuggestionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        DocsNoteSuggestion suggestion = new DocsNoteSuggestion();
        suggestion.setNoteId(noteId);
        suggestion.setAuthorUserId(userId);
        suggestion.setSelectionStart(request.selectionStart());
        suggestion.setSelectionEnd(request.selectionEnd());
        suggestion.setOriginalText(request.originalText());
        suggestion.setReplacementText(normalizeReplacementText(request.replacementText()));
        suggestion.setBaseVersion(request.baseVersion());
        suggestion.setStatus(STATUS_PENDING);
        suggestion.setCreatedAt(now);
        suggestion.setUpdatedAt(now);
        suggestion.setDeleted(0);
        docsNoteSuggestionMapper.insert(suggestion);
        return suggestion;
    }

    private DocsNoteSuggestion requirePendingSuggestion(Long noteId, Long suggestionId) {
        DocsNoteSuggestion suggestion = docsNoteSuggestionMapper.selectOne(new LambdaQueryWrapper<DocsNoteSuggestion>()
                .eq(DocsNoteSuggestion::getId, suggestionId)
                .eq(DocsNoteSuggestion::getNoteId, noteId));
        if (suggestion == null) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_NOT_FOUND);
        }
        if (!STATUS_PENDING.equals(suggestion.getStatus())) {
            throw new BizException(ErrorCode.DOCS_NOTE_SUGGESTION_CONFLICT, "Suggestion has already been resolved");
        }
        return suggestion;
    }

    private void applySuggestion(DocsNote note, DocsNoteSuggestion suggestion) {
        String content = note.getContent() == null ? "" : note.getContent();
        String updatedContent = content.substring(0, suggestion.getSelectionStart())
                + suggestion.getReplacementText()
                + content.substring(suggestion.getSelectionEnd());
        note.setContent(updatedContent);
        note.setCurrentVersion(note.getCurrentVersion() + 1);
        note.setUpdatedAt(LocalDateTime.now());
        docsNoteMapper.updateById(note);
    }

    private void resolveSuggestion(DocsNoteSuggestion suggestion, Long userId, String status) {
        LocalDateTime now = LocalDateTime.now();
        suggestion.setStatus(status);
        suggestion.setResolvedByUserId(userId);
        suggestion.setResolvedAt(now);
        suggestion.setUpdatedAt(now);
        docsNoteSuggestionMapper.updateById(suggestion);
    }

    private void publish(Long noteId, AuditEventVo event) {
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
    }

    private List<DocsNoteSuggestionVo> toSuggestionVos(List<DocsNoteSuggestion> suggestions) {
        Set<Long> userIds = suggestions.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getAuthorUserId(), item.getResolvedByUserId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserAccount> userMap = loadUserMap(userIds);
        return suggestions.stream().map(suggestion -> {
            UserAccount author = userMap.get(suggestion.getAuthorUserId());
            UserAccount resolver = userMap.get(suggestion.getResolvedByUserId());
            return new DocsNoteSuggestionVo(
                    String.valueOf(suggestion.getId()),
                    String.valueOf(suggestion.getAuthorUserId()),
                    author == null ? "" : author.getEmail(),
                    author == null ? "" : author.getDisplayName(),
                    suggestion.getStatus(),
                    suggestion.getSelectionStart(),
                    suggestion.getSelectionEnd(),
                    suggestion.getOriginalText(),
                    suggestion.getReplacementText(),
                    suggestion.getBaseVersion(),
                    suggestion.getResolvedByUserId() == null ? null : String.valueOf(suggestion.getResolvedByUserId()),
                    resolver == null ? null : resolver.getEmail(),
                    resolver == null ? null : resolver.getDisplayName(),
                    suggestion.getResolvedAt(),
                    suggestion.getCreatedAt()
            );
        }).toList();
    }

    private Map<Long, UserAccount> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, item -> item));
    }

    private String normalizeReplacementText(String replacementText) {
        return StringUtils.hasText(replacementText) ? replacementText.trim() : "";
    }

    private String buildAuditDetail(Long noteId, Long sessionId, String actorEmail, List<String> extraParts) {
        List<String> parts = new java.util.ArrayList<>();
        parts.add("noteId=" + noteId);
        parts.add("sessionId=" + sessionId);
        parts.add("actorEmail=" + actorEmail.trim().toLowerCase());
        parts.addAll(extraParts);
        return String.join(";", parts) + ";";
    }
}
