package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteCommentMapper;
import com.mmmail.server.mapper.DocsNotePresenceMapper;
import com.mmmail.server.mapper.DocsNoteShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateDocsNoteCommentRequest;
import com.mmmail.server.model.dto.CreateDocsNoteShareRequest;
import com.mmmail.server.model.dto.HeartbeatDocsNotePresenceRequest;
import com.mmmail.server.model.dto.UpdateDocsNoteShareRequest;
import com.mmmail.server.model.entity.DocsNoteComment;
import com.mmmail.server.model.entity.DocsNotePresence;
import com.mmmail.server.model.entity.DocsNoteShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DocsNoteCollaborationOverviewVo;
import com.mmmail.server.model.vo.DocsNoteCommentVo;
import com.mmmail.server.model.vo.DocsNotePresenceVo;
import com.mmmail.server.model.vo.DocsNoteShareVo;
import com.mmmail.server.model.vo.DocsNoteSyncVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocsCollaborationService {

    private static final int PRESENCE_TTL_SECONDS = 90;
    private static final String MODE_VIEW = "VIEW";
    private static final String MODE_EDIT = "EDIT";

    private final DocsAccessService docsAccessService;
    private final DocsNoteShareMapper docsNoteShareMapper;
    private final DocsNoteCommentMapper docsNoteCommentMapper;
    private final DocsNotePresenceMapper docsNotePresenceMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final DocsCollaborationSyncService docsCollaborationSyncService;
    private final SuiteCollaborationService suiteCollaborationService;

    public DocsCollaborationService(
            DocsAccessService docsAccessService,
            DocsNoteShareMapper docsNoteShareMapper,
            DocsNoteCommentMapper docsNoteCommentMapper,
            DocsNotePresenceMapper docsNotePresenceMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            DocsCollaborationSyncService docsCollaborationSyncService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.docsAccessService = docsAccessService;
        this.docsNoteShareMapper = docsNoteShareMapper;
        this.docsNoteCommentMapper = docsNoteCommentMapper;
        this.docsNotePresenceMapper = docsNotePresenceMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.docsCollaborationSyncService = docsCollaborationSyncService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    public DocsNoteCollaborationOverviewVo getOverview(Long userId, Long noteId) {
        docsAccessService.requireAccessible(userId, noteId);
        long syncCursor = docsCollaborationSyncService.getCurrentCursor(noteId);
        return new DocsNoteCollaborationOverviewVo(
                LocalDateTime.now(),
                listShares(userId, noteId),
                listComments(userId, noteId, true),
                listPresence(userId, noteId),
                syncCursor,
                docsCollaborationSyncService.buildSyncVersion(syncCursor)
        );
    }

    public List<DocsNoteShareVo> listShares(Long userId, Long noteId) {
        docsAccessService.requireAccessible(userId, noteId);
        List<DocsNoteShare> shares = docsNoteShareMapper.selectList(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId)
                .orderByAsc(DocsNoteShare::getCreatedAt));
        return toShareVos(shares);
    }

    public DocsNoteShareVo createShare(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            CreateDocsNoteShareRequest request,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireOwned(userId, noteId);
        UserAccount collaborator = loadCollaborator(request.collaboratorEmail());
        ensureNotSelf(context.note().getOwnerId(), collaborator.getId());
        ensureShareNotExists(noteId, collaborator.getId());
        DocsNoteShare share = insertShare(context.note().getOwnerId(), noteId, collaborator.getId(), request.permission());
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SHARE_ADD",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of(
                        "shareId=" + share.getId(),
                        "collaboratorEmail=" + collaborator.getEmail(),
                        "permission=" + request.permission()
                )),
                ipAddress
        );
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
        return toShareVos(List.of(share)).getFirst();
    }

    public DocsNoteShareVo removeShare(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            Long shareId,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireOwned(userId, noteId);
        DocsNoteShare share = loadShareOwnedByNote(context.note().getOwnerId(), noteId, shareId);
        docsNoteShareMapper.deleteById(share.getId());
        DocsNoteShareVo removed = toShareVos(List.of(share)).getFirst();
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SHARE_REMOVE",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of(
                        "shareId=" + shareId,
                        "collaboratorEmail=" + removed.collaboratorEmail(),
                        "permission=" + share.getPermission()
                )),
                ipAddress
        );
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
        return removed;
    }

    public DocsNoteShareVo updateSharePermission(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            Long shareId,
            UpdateDocsNoteShareRequest request,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireOwned(userId, noteId);
        DocsNoteShare share = loadShareOwnedByNote(context.note().getOwnerId(), noteId, shareId);
        share.setPermission(request.permission());
        share.setUpdatedAt(LocalDateTime.now());
        docsNoteShareMapper.updateById(share);
        DocsNoteShareVo updated = toShareVos(List.of(share)).getFirst();
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_SHARE_PERMISSION_UPDATE",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of(
                        "shareId=" + shareId,
                        "collaboratorEmail=" + updated.collaboratorEmail(),
                        "permission=" + share.getPermission()
                )),
                ipAddress
        );
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
        return updated;
    }

    public List<DocsNoteCommentVo> listComments(Long userId, Long noteId, boolean includeResolved) {
        docsAccessService.requireAccessible(userId, noteId);
        LambdaQueryWrapper<DocsNoteComment> query = new LambdaQueryWrapper<DocsNoteComment>()
                .eq(DocsNoteComment::getNoteId, noteId)
                .orderByDesc(DocsNoteComment::getCreatedAt);
        if (!includeResolved) {
            query.eq(DocsNoteComment::getResolved, 0);
        }
        return toCommentVos(docsNoteCommentMapper.selectList(query));
    }

    public DocsNoteCommentVo createComment(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            CreateDocsNoteCommentRequest request,
            String ipAddress
    ) {
        docsAccessService.requireAccessible(userId, noteId);
        DocsNoteComment comment = insertComment(userId, noteId, request.excerpt(), request.content());
        AuditEventVo event = auditService.recordEvent(
                userId,
                "DOCS_NOTE_COMMENT_ADD",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of("commentId=" + comment.getId())),
                ipAddress
        );
        docsCollaborationSyncService.publish(noteId, event);
        suiteCollaborationService.publishToDocsRecipients(noteId, event);
        return toCommentVos(List.of(comment)).getFirst();
    }

    public DocsNoteCommentVo resolveComment(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            Long commentId,
            String ipAddress
    ) {
        docsAccessService.requireEditable(userId, noteId);
        DocsNoteComment comment = loadComment(noteId, commentId);
        if (comment.getResolved() == null || comment.getResolved() == 0) {
            LocalDateTime now = LocalDateTime.now();
            comment.setResolved(1);
            comment.setResolvedAt(now);
            comment.setUpdatedAt(now);
            docsNoteCommentMapper.updateById(comment);
            AuditEventVo event = auditService.recordEvent(
                    userId,
                    "DOCS_NOTE_COMMENT_RESOLVE",
                    buildAuditDetail(noteId, sessionId, actorEmail, List.of("commentId=" + commentId)),
                    ipAddress
            );
            docsCollaborationSyncService.publish(noteId, event);
            suiteCollaborationService.publishToDocsRecipients(noteId, event);
        }
        return toCommentVos(List.of(comment)).getFirst();
    }

    public DocsNotePresenceVo heartbeat(
            Long userId,
            String actorEmail,
            Long sessionId,
            Long noteId,
            HeartbeatDocsNotePresenceRequest request,
            String ipAddress
    ) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireAccessible(userId, noteId);
        validatePresenceMode(context, request.activeMode());
        DocsNotePresence presence = upsertPresence(userId, sessionId, noteId, request.activeMode());
        auditService.record(
                userId,
                "DOCS_NOTE_PRESENCE_HEARTBEAT",
                buildAuditDetail(noteId, sessionId, actorEmail, List.of("activeMode=" + request.activeMode())),
                ipAddress
        );
        return toPresenceVos(List.of(presence), noteId, context.note().getOwnerId()).getFirst();
    }

    public List<DocsNotePresenceVo> listPresence(Long userId, Long noteId) {
        DocsAccessService.DocsAccessContext context = docsAccessService.requireAccessible(userId, noteId);
        List<DocsNotePresence> presenceItems = docsNotePresenceMapper.selectList(new LambdaQueryWrapper<DocsNotePresence>()
                .eq(DocsNotePresence::getNoteId, noteId)
                .ge(DocsNotePresence::getLastHeartbeatAt, LocalDateTime.now().minusSeconds(PRESENCE_TTL_SECONDS))
                .orderByDesc(DocsNotePresence::getLastHeartbeatAt));
        return toPresenceVos(presenceItems, noteId, context.note().getOwnerId());
    }

    public DocsNoteSyncVo getSync(Long userId, Long noteId, Long afterEventId, Integer limit) {
        docsAccessService.requireAccessible(userId, noteId);
        return docsCollaborationSyncService.getSync(noteId, afterEventId, limit);
    }

    public SseEmitter openSyncStream(Long userId, Long noteId, Long afterEventId) {
        docsAccessService.requireAccessible(userId, noteId);
        return docsCollaborationSyncService.openStream(noteId, afterEventId);
    }

    private UserAccount loadCollaborator(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        UserAccount collaborator = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
        if (collaborator == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "Collaborator email is not registered");
        }
        return collaborator;
    }

    private void ensureNotSelf(Long ownerId, Long collaboratorId) {
        if (ownerId.equals(collaboratorId)) {
            throw new BizException(ErrorCode.DOCS_NOTE_SHARE_CONFLICT, "Owner cannot share note to self");
        }
    }

    private void ensureShareNotExists(Long noteId, Long collaboratorId) {
        Long count = docsNoteShareMapper.selectCount(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId)
                .eq(DocsNoteShare::getCollaboratorUserId, collaboratorId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DOCS_NOTE_SHARE_CONFLICT, "Collaborator already has access");
        }
    }

    private DocsNoteShare insertShare(Long ownerId, Long noteId, Long collaboratorId, String permission) {
        LocalDateTime now = LocalDateTime.now();
        DocsNoteShare share = new DocsNoteShare();
        share.setNoteId(noteId);
        share.setOwnerId(ownerId);
        share.setCollaboratorUserId(collaboratorId);
        share.setPermission(permission);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);
        share.setDeleted(0);
        docsNoteShareMapper.insert(share);
        return share;
    }

    private DocsNoteComment insertComment(Long authorUserId, Long noteId, String excerpt, String content) {
        LocalDateTime now = LocalDateTime.now();
        DocsNoteComment comment = new DocsNoteComment();
        comment.setNoteId(noteId);
        comment.setAuthorUserId(authorUserId);
        comment.setExcerpt(normalizeExcerpt(excerpt));
        comment.setContent(content.trim());
        comment.setResolved(0);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setDeleted(0);
        docsNoteCommentMapper.insert(comment);
        return comment;
    }

    private DocsNoteShare loadShareOwnedByNote(Long ownerId, Long noteId, Long shareId) {
        DocsNoteShare share = docsNoteShareMapper.selectOne(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getId, shareId)
                .eq(DocsNoteShare::getNoteId, noteId)
                .eq(DocsNoteShare::getOwnerId, ownerId));
        if (share == null) {
            throw new BizException(ErrorCode.DOCS_NOTE_SHARE_NOT_FOUND);
        }
        return share;
    }

    private DocsNoteComment loadComment(Long noteId, Long commentId) {
        DocsNoteComment comment = docsNoteCommentMapper.selectOne(new LambdaQueryWrapper<DocsNoteComment>()
                .eq(DocsNoteComment::getId, commentId)
                .eq(DocsNoteComment::getNoteId, noteId));
        if (comment == null) {
            throw new BizException(ErrorCode.DOCS_NOTE_COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private DocsNotePresence upsertPresence(Long userId, Long sessionId, Long noteId, String activeMode) {
        DocsNotePresence presence = docsNotePresenceMapper.selectOne(new LambdaQueryWrapper<DocsNotePresence>()
                .eq(DocsNotePresence::getNoteId, noteId)
                .eq(DocsNotePresence::getUserId, userId)
                .eq(DocsNotePresence::getSessionId, sessionId));
        LocalDateTime now = LocalDateTime.now();
        if (presence == null) {
            return insertPresence(userId, sessionId, noteId, activeMode, now);
        }
        docsNotePresenceMapper.update(null, new LambdaUpdateWrapper<DocsNotePresence>()
                .eq(DocsNotePresence::getId, presence.getId())
                .set(DocsNotePresence::getActiveMode, activeMode)
                .set(DocsNotePresence::getLastHeartbeatAt, now)
                .set(DocsNotePresence::getUpdatedAt, now));
        presence.setActiveMode(activeMode);
        presence.setLastHeartbeatAt(now);
        presence.setUpdatedAt(now);
        return presence;
    }

    private DocsNotePresence insertPresence(Long userId, Long sessionId, Long noteId, String activeMode, LocalDateTime now) {
        DocsNotePresence presence = new DocsNotePresence();
        presence.setNoteId(noteId);
        presence.setUserId(userId);
        presence.setSessionId(sessionId);
        presence.setActiveMode(activeMode);
        presence.setLastHeartbeatAt(now);
        presence.setCreatedAt(now);
        presence.setUpdatedAt(now);
        presence.setDeleted(0);
        docsNotePresenceMapper.insert(presence);
        return presence;
    }

    private void validatePresenceMode(DocsAccessService.DocsAccessContext context, String activeMode) {
        if (MODE_EDIT.equals(activeMode) && !context.canEdit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Read-only collaborator cannot send EDIT presence");
        }
    }

    private List<DocsNoteShareVo> toShareVos(List<DocsNoteShare> shares) {
        Map<Long, UserAccount> userMap = loadUserMap(shares.stream().map(DocsNoteShare::getCollaboratorUserId).collect(Collectors.toSet()));
        return shares.stream().map(share -> {
            UserAccount user = userMap.get(share.getCollaboratorUserId());
            return new DocsNoteShareVo(
                    String.valueOf(share.getId()),
                    String.valueOf(share.getCollaboratorUserId()),
                    user == null ? "" : user.getEmail(),
                    user == null ? "" : user.getDisplayName(),
                    share.getPermission(),
                    share.getCreatedAt()
            );
        }).toList();
    }

    private List<DocsNoteCommentVo> toCommentVos(List<DocsNoteComment> comments) {
        Map<Long, UserAccount> userMap = loadUserMap(comments.stream().map(DocsNoteComment::getAuthorUserId).collect(Collectors.toSet()));
        return comments.stream().map(comment -> {
            UserAccount user = userMap.get(comment.getAuthorUserId());
            return new DocsNoteCommentVo(
                    String.valueOf(comment.getId()),
                    String.valueOf(comment.getAuthorUserId()),
                    user == null ? "" : user.getEmail(),
                    user == null ? "" : user.getDisplayName(),
                    comment.getExcerpt(),
                    comment.getContent(),
                    comment.getResolved() != null && comment.getResolved() == 1,
                    comment.getResolvedAt(),
                    comment.getCreatedAt()
            );
        }).toList();
    }

    private List<DocsNotePresenceVo> toPresenceVos(List<DocsNotePresence> items, Long noteId, Long ownerId) {
        Map<Long, UserAccount> userMap = loadUserMap(items.stream().map(DocsNotePresence::getUserId).collect(Collectors.toSet()));
        Map<Long, String> permissionMap = loadPresencePermissionMap(items, noteId, ownerId);
        return items.stream().map(item -> {
            UserAccount user = userMap.get(item.getUserId());
            return new DocsNotePresenceVo(
                    String.valueOf(item.getId()),
                    String.valueOf(item.getUserId()),
                    user == null ? "" : user.getEmail(),
                    user == null ? "" : user.getDisplayName(),
                    String.valueOf(item.getSessionId()),
                    item.getActiveMode(),
                    permissionMap.getOrDefault(item.getUserId(), DocsAccessService.PERMISSION_VIEW),
                    item.getLastHeartbeatAt()
            );
        }).toList();
    }

    private Map<Long, UserAccount> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, user -> user));
    }

    private Map<Long, String> loadPresencePermissionMap(List<DocsNotePresence> items, Long noteId, Long ownerId) {
        Set<Long> userIds = items.stream().map(DocsNotePresence::getUserId).collect(Collectors.toSet());
        Map<Long, String> permissionMap = userIds.stream().collect(Collectors.toMap(id -> id, id -> DocsAccessService.PERMISSION_VIEW));
        permissionMap.put(ownerId, DocsAccessService.PERMISSION_OWNER);
        if (userIds.isEmpty()) {
            return permissionMap;
        }
        List<DocsNoteShare> shares = docsNoteShareMapper.selectList(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId)
                .eq(DocsNoteShare::getOwnerId, ownerId)
                .in(DocsNoteShare::getCollaboratorUserId, userIds));
        for (DocsNoteShare share : shares) {
            permissionMap.put(share.getCollaboratorUserId(), share.getPermission());
        }
        return permissionMap;
    }

    private String buildAuditDetail(Long noteId, Long sessionId, String actorEmail, List<String> extraParts) {
        List<String> parts = new java.util.ArrayList<>();
        parts.add("noteId=" + noteId);
        parts.add("sessionId=" + sessionId);
        parts.add("actorEmail=" + normalizeEmail(actorEmail));
        parts.addAll(extraParts);
        return String.join(";", parts) + ";";
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeExcerpt(String excerpt) {
        if (!StringUtils.hasText(excerpt)) {
            return null;
        }
        return excerpt.trim();
    }
}
