package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteCommentMapper;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DocsNotePresenceMapper;
import com.mmmail.server.mapper.DocsNoteShareMapper;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DocsNoteDetailVo;
import com.mmmail.server.model.vo.DocsNoteSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocsService {

    private final DocsNoteMapper docsNoteMapper;
    private final DocsNoteShareMapper docsNoteShareMapper;
    private final DocsNoteCommentMapper docsNoteCommentMapper;
    private final DocsNotePresenceMapper docsNotePresenceMapper;
    private final AuditService auditService;
    private final DocsAccessService docsAccessService;
    private final DocsCollaborationSyncService docsCollaborationSyncService;
    private final SuiteCollaborationService suiteCollaborationService;

    public DocsService(
            DocsNoteMapper docsNoteMapper,
            DocsNoteShareMapper docsNoteShareMapper,
            DocsNoteCommentMapper docsNoteCommentMapper,
            DocsNotePresenceMapper docsNotePresenceMapper,
            AuditService auditService,
            DocsAccessService docsAccessService,
            DocsCollaborationSyncService docsCollaborationSyncService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.docsNoteMapper = docsNoteMapper;
        this.docsNoteShareMapper = docsNoteShareMapper;
        this.docsNoteCommentMapper = docsNoteCommentMapper;
        this.docsNotePresenceMapper = docsNotePresenceMapper;
        this.auditService = auditService;
        this.docsAccessService = docsAccessService;
        this.docsCollaborationSyncService = docsCollaborationSyncService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    public List<DocsNoteSummaryVo> list(Long userId, String keyword, Integer limit) {
        return docsAccessService.listVisibleNotes(userId, keyword, limit);
    }

    @Transactional
    public DocsNoteDetailVo create(Long userId, String actorEmail, Long sessionId, String title, String content, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        DocsNote note = new DocsNote();
        note.setOwnerId(userId);
        note.setWorkspaceType("DOCS");
        note.setTitle(requireTitle(title));
        note.setContent(normalizeContent(content));
        note.setCurrentVersion(1);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        note.setDeleted(0);
        docsNoteMapper.insert(note);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_CREATE",
                buildAuditDetail(note.getId(), sessionId, actorEmail, List.of("version=1")),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toDetailVo(docsAccessService.requireOwned(userId, note.getId()));
    }

    public DocsNoteDetailVo get(Long userId, Long noteId) {
        return toDetailVo(docsAccessService.requireAccessible(userId, noteId));
    }

    @Transactional
    public DocsNoteDetailVo update(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            String title,
            String content,
            Integer currentVersion,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireEditable(userId, noteId);
        validateCurrentVersion(context.note(), currentVersion);
        DocsNote note = context.note();
        note.setTitle(requireTitle(title));
        note.setContent(normalizeContent(content));
        note.setCurrentVersion(note.getCurrentVersion() + 1);
        note.setUpdatedAt(LocalDateTime.now());
        docsNoteMapper.updateById(note);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_UPDATE",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of("version=" + note.getCurrentVersion())),
                ipAddress
        );
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
        return toDetailVo(docsAccessService.requireAccessible(userId, noteId));
    }

    @Transactional
    public void delete(Long userId, String actorEmail, Long sessionId, Long noteId, String ipAddress) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireOwned(userId, noteId);
        docsNoteMapper.deleteById(context.note().getId());
        docsNoteShareMapper.delete(new LambdaQueryWrapper<com.mmmail.server.model.entity.DocsNoteShare>()
                .eq(com.mmmail.server.model.entity.DocsNoteShare::getNoteId, noteId));
        docsNoteCommentMapper.delete(new LambdaQueryWrapper<com.mmmail.server.model.entity.DocsNoteComment>()
                .eq(com.mmmail.server.model.entity.DocsNoteComment::getNoteId, noteId));
        docsNotePresenceMapper.delete(new LambdaQueryWrapper<com.mmmail.server.model.entity.DocsNotePresence>()
                .eq(com.mmmail.server.model.entity.DocsNotePresence::getNoteId, noteId));
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_DELETE",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of()),
                ipAddress
        );
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
    }

    private void validateCurrentVersion(DocsNote note, Integer currentVersion) {
        if (currentVersion == null || currentVersion < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "currentVersion is required");
        }
        if (!currentVersion.equals(note.getCurrentVersion())) {
            throw new BizException(
                    ErrorCode.DOCS_NOTE_VERSION_CONFLICT,
                    "Docs note has been updated by another collaborator"
            );
        }
    }

    private String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Docs title is required");
        }
        return title.trim();
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private DocsNoteDetailVo toDetailVo(DocsAccessService.DocsAccessContext context) {
        DocsNote note = loadFreshNote(context.note().getId());
        long syncCursor = docsCollaborationSyncService.getCurrentCursor(note.getId());
        return new DocsNoteDetailVo(
                String.valueOf(note.getId()),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getCurrentVersion(),
                context.permission(),
                context.shared(),
                context.owner().getEmail(),
                context.owner().getDisplayName(),
                docsAccessService.countCollaborators(note.getId()),
                syncCursor,
                docsCollaborationSyncService.buildSyncVersion(syncCursor)
        );
    }

    private DocsNote loadFreshNote(Long noteId) {
        DocsNote note = docsNoteMapper.selectById(noteId);
        if (note == null) {
            throw new BizException(ErrorCode.DOCS_NOTE_NOT_FOUND);
        }
        return note;
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
