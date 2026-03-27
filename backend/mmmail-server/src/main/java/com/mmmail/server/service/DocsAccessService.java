package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DocsNoteShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.DocsNoteShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.DocsNoteSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocsAccessService {

    public static final String PERMISSION_OWNER = "OWNER";
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    private static final String SCOPE_OWNED = "OWNED";
    private static final String SCOPE_SHARED = "SHARED";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final DocsNoteMapper docsNoteMapper;
    private final DocsNoteShareMapper docsNoteShareMapper;
    private final UserAccountMapper userAccountMapper;

    public DocsAccessService(
            DocsNoteMapper docsNoteMapper,
            DocsNoteShareMapper docsNoteShareMapper,
            UserAccountMapper userAccountMapper
    ) {
        this.docsNoteMapper = docsNoteMapper;
        this.docsNoteShareMapper = docsNoteShareMapper;
        this.userAccountMapper = userAccountMapper;
    }

    public List<DocsNoteSummaryVo> listVisibleNotes(Long userId, String keyword, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        String normalizedKeyword = normalizeKeyword(keyword);
        List<DocsNote> ownedNotes = listOwnedNotes(userId, normalizedKeyword, safeLimit);
        List<DocsNoteShare> sharedRows = listSharedRows(userId);
        List<DocsNote> sharedNotes = listSharedNotes(sharedRows, normalizedKeyword);
        Map<Long, DocsNoteShare> shareByNoteId = sharedRows.stream()
                .collect(Collectors.toMap(DocsNoteShare::getNoteId, item -> item, (left, right) -> left));
        Map<Long, Integer> collaboratorCountMap = loadCollaboratorCountMap(ownedNotes, sharedNotes);
        Map<Long, UserAccount> ownerMap = loadOwnerMap(ownedNotes, sharedNotes);
        return mergeAndSortNotes(ownedNotes, sharedNotes).stream()
                .limit(safeLimit)
                .map(note -> toSummaryVo(note, shareByNoteId.get(note.getId()), ownerMap.get(note.getOwnerId()), collaboratorCountMap))
                .toList();
    }

    public DocsAccessContext requireAccessible(Long userId, Long noteId) {
        DocsNote note = loadNote(noteId);
        if (note.getOwnerId().equals(userId)) {
            return new DocsAccessContext(note, loadUser(note.getOwnerId()), PERMISSION_OWNER, false);
        }
        DocsNoteShare share = loadShare(noteId, userId);
        if (share == null) {
            throw new BizException(ErrorCode.DOCS_NOTE_NOT_FOUND);
        }
        return new DocsAccessContext(note, loadUser(note.getOwnerId()), share.getPermission(), true);
    }

    public DocsAccessContext requireEditable(Long userId, Long noteId) {
        DocsAccessContext context = requireAccessible(userId, noteId);
        if (!context.canEdit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Docs note is read-only for current user");
        }
        return context;
    }

    public DocsAccessContext requireOwned(Long userId, Long noteId) {
        DocsAccessContext context = requireAccessible(userId, noteId);
        if (!context.isOwner()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Only note owner can perform this action");
        }
        return context;
    }

    public int countCollaborators(Long noteId) {
        Long count = docsNoteShareMapper.selectCount(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId));
        return count == null ? 0 : count.intValue();
    }

    private List<DocsNote> listOwnedNotes(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<DocsNote> query = new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "DOCS")
                .orderByDesc(DocsNote::getUpdatedAt)
                .last("limit " + limit);
        if (StringUtils.hasText(keyword)) {
            query.like(DocsNote::getTitle, keyword);
        }
        return docsNoteMapper.selectList(query);
    }

    private List<DocsNoteShare> listSharedRows(Long userId) {
        return docsNoteShareMapper.selectList(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getCollaboratorUserId, userId)
                .orderByDesc(DocsNoteShare::getUpdatedAt));
    }

    private List<DocsNote> listSharedNotes(List<DocsNoteShare> sharedRows, String keyword) {
        Set<Long> noteIds = sharedRows.stream().map(DocsNoteShare::getNoteId).collect(Collectors.toSet());
        if (noteIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<DocsNote> query = new LambdaQueryWrapper<DocsNote>()
                .in(DocsNote::getId, noteIds)
                .eq(DocsNote::getWorkspaceType, "DOCS")
                .orderByDesc(DocsNote::getUpdatedAt);
        if (StringUtils.hasText(keyword)) {
            query.like(DocsNote::getTitle, keyword);
        }
        return docsNoteMapper.selectList(query);
    }

    private List<DocsNote> mergeAndSortNotes(List<DocsNote> ownedNotes, List<DocsNote> sharedNotes) {
        Map<Long, DocsNote> merged = new LinkedHashMap<>();
        for (DocsNote note : ownedNotes) {
            merged.put(note.getId(), note);
        }
        for (DocsNote note : sharedNotes) {
            merged.putIfAbsent(note.getId(), note);
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(DocsNote::getUpdatedAt).reversed())
                .toList();
    }

    private Map<Long, Integer> loadCollaboratorCountMap(List<DocsNote> ownedNotes, List<DocsNote> sharedNotes) {
        Set<Long> noteIds = mergeAndSortNotes(ownedNotes, sharedNotes).stream()
                .map(DocsNote::getId)
                .collect(Collectors.toSet());
        if (noteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        List<DocsNoteShare> shares = docsNoteShareMapper.selectList(new LambdaQueryWrapper<DocsNoteShare>()
                .in(DocsNoteShare::getNoteId, noteIds));
        for (DocsNoteShare share : shares) {
            result.merge(share.getNoteId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, UserAccount> loadOwnerMap(List<DocsNote> ownedNotes, List<DocsNote> sharedNotes) {
        Set<Long> ownerIds = mergeAndSortNotes(ownedNotes, sharedNotes).stream()
                .map(DocsNote::getOwnerId)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, ownerIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, item -> item));
    }

    private DocsNote loadNote(Long noteId) {
        DocsNote note = docsNoteMapper.selectById(noteId);
        if (note == null || !"DOCS".equals(note.getWorkspaceType())) {
            throw new BizException(ErrorCode.DOCS_NOTE_NOT_FOUND);
        }
        return note;
    }

    private DocsNoteShare loadShare(Long noteId, Long collaboratorUserId) {
        return docsNoteShareMapper.selectOne(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId)
                .eq(DocsNoteShare::getCollaboratorUserId, collaboratorUserId));
    }

    private UserAccount loadUser(Long userId) {
        UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return account;
    }

    private DocsNoteSummaryVo toSummaryVo(
            DocsNote note,
            DocsNoteShare share,
            UserAccount owner,
            Map<Long, Integer> collaboratorCountMap
    ) {
        String permission = share == null ? PERMISSION_OWNER : share.getPermission();
        String scope = share == null ? SCOPE_OWNED : SCOPE_SHARED;
        int collaboratorCount = collaboratorCountMap.getOrDefault(note.getId(), 0);
        return new DocsNoteSummaryVo(
                String.valueOf(note.getId()),
                note.getTitle(),
                note.getUpdatedAt(),
                permission,
                scope,
                note.getCurrentVersion() == null ? 1 : note.getCurrentVersion(),
                owner == null ? "" : owner.getEmail(),
                owner == null ? "" : owner.getDisplayName(),
                collaboratorCount
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    public record DocsAccessContext(DocsNote note, UserAccount owner, String permission, boolean shared) {

        public boolean canEdit() {
            return PERMISSION_OWNER.equals(permission) || PERMISSION_EDIT.equals(permission);
        }

        public boolean isOwner() {
            return PERMISSION_OWNER.equals(permission);
        }
    }
}
