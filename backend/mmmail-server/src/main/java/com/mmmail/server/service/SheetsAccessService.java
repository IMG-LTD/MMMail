package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.mapper.SheetsWorkbookShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.entity.SheetsWorkbookShare;
import com.mmmail.server.model.entity.UserAccount;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SheetsAccessService {

    public static final String PERMISSION_OWNER = "OWNER";
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String SCOPE_OWNED = "OWNED";
    public static final String SCOPE_SHARED = "SHARED";
    public static final String SHARE_STATUS_ACCEPTED = "ACCEPTED";
    private static final String SHARE_STATUS_DECLINED = "DECLINED";
    private static final int DEFAULT_LIMIT = 40;
    private static final int MAX_LIMIT = 100;

    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final SheetsWorkbookShareMapper sheetsWorkbookShareMapper;
    private final UserAccountMapper userAccountMapper;

    public SheetsAccessService(
            SheetsWorkbookMapper sheetsWorkbookMapper,
            SheetsWorkbookShareMapper sheetsWorkbookShareMapper,
            UserAccountMapper userAccountMapper
    ) {
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.sheetsWorkbookShareMapper = sheetsWorkbookShareMapper;
        this.userAccountMapper = userAccountMapper;
    }

    public List<SheetsWorkbookAccessContext> listVisibleWorkbooks(Long userId, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        List<SheetsWorkbook> owned = listOwnedWorkbooks(userId, safeLimit);
        List<SheetsWorkbookShare> shares = listAcceptedShares(userId);
        List<SheetsWorkbook> shared = listSharedWorkbooks(shares);
        List<SheetsWorkbook> merged = mergeAndSortWorkbooks(owned, shared);
        Map<Long, SheetsWorkbookShare> shareByWorkbookId = toShareMap(shares);
        Map<Long, UserAccount> ownerMap = loadOwnerMap(merged);
        Map<Long, Integer> collaboratorCountMap = loadCollaboratorCountMap(merged);
        return merged.stream()
                .limit(safeLimit)
                .map(workbook -> toContext(
                        workbook,
                        ownerMap.get(workbook.getOwnerId()),
                        shareByWorkbookId.get(workbook.getId()),
                        collaboratorCountMap.getOrDefault(workbook.getId(), 0)
                ))
                .toList();
    }

    public SheetsWorkbookAccessContext requireAccessible(Long userId, Long workbookId) {
        SheetsWorkbook workbook = loadWorkbook(workbookId);
        if (workbook.getOwnerId().equals(userId)) {
            return ownedContext(workbook);
        }
        SheetsWorkbookShare share = sheetsWorkbookShareMapper.selectOne(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getWorkbookId, workbookId)
                .eq(SheetsWorkbookShare::getCollaboratorUserId, userId)
                .eq(SheetsWorkbookShare::getResponseStatus, SHARE_STATUS_ACCEPTED)
                .last("limit 1"));
        if (share == null) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_NOT_FOUND);
        }
        return sharedContext(workbook, share);
    }

    public SheetsWorkbookAccessContext requireEditable(Long userId, Long workbookId) {
        SheetsWorkbookAccessContext context = requireAccessible(userId, workbookId);
        if (!context.canEdit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Sheets workbook is read-only for current user");
        }
        return context;
    }

    public SheetsWorkbookAccessContext requireOwned(Long userId, Long workbookId) {
        SheetsWorkbookAccessContext context = requireAccessible(userId, workbookId);
        if (!context.isOwner()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Only workbook owner can perform this action");
        }
        return context;
    }

    private SheetsWorkbookAccessContext ownedContext(SheetsWorkbook workbook) {
        return new SheetsWorkbookAccessContext(
                workbook,
                loadUser(workbook.getOwnerId()),
                PERMISSION_OWNER,
                false,
                countCollaborators(workbook.getId())
        );
    }

    private SheetsWorkbookAccessContext sharedContext(SheetsWorkbook workbook, SheetsWorkbookShare share) {
        return new SheetsWorkbookAccessContext(
                workbook,
                loadUser(workbook.getOwnerId()),
                share.getPermission(),
                true,
                countCollaborators(workbook.getId())
        );
    }

    private List<SheetsWorkbook> listOwnedWorkbooks(Long userId, int limit) {
        return sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>()
                .eq(SheetsWorkbook::getOwnerId, userId)
                .orderByDesc(SheetsWorkbook::getLastOpenedAt)
                .orderByDesc(SheetsWorkbook::getUpdatedAt)
                .last("limit " + limit));
    }

    private List<SheetsWorkbookShare> listAcceptedShares(Long userId) {
        return sheetsWorkbookShareMapper.selectList(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getCollaboratorUserId, userId)
                .eq(SheetsWorkbookShare::getResponseStatus, SHARE_STATUS_ACCEPTED)
                .orderByDesc(SheetsWorkbookShare::getUpdatedAt));
    }

    private List<SheetsWorkbook> listSharedWorkbooks(List<SheetsWorkbookShare> shares) {
        Set<Long> workbookIds = shares.stream().map(SheetsWorkbookShare::getWorkbookId).collect(java.util.stream.Collectors.toSet());
        if (workbookIds.isEmpty()) {
            return List.of();
        }
        return sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>()
                .in(SheetsWorkbook::getId, workbookIds)
                .orderByDesc(SheetsWorkbook::getLastOpenedAt)
                .orderByDesc(SheetsWorkbook::getUpdatedAt));
    }

    private List<SheetsWorkbook> mergeAndSortWorkbooks(List<SheetsWorkbook> owned, List<SheetsWorkbook> shared) {
        Map<Long, SheetsWorkbook> merged = new LinkedHashMap<>();
        for (SheetsWorkbook workbook : owned) {
            merged.put(workbook.getId(), workbook);
        }
        for (SheetsWorkbook workbook : shared) {
            merged.putIfAbsent(workbook.getId(), workbook);
        }
        return merged.values().stream()
                .sorted(Comparator
                        .comparing(SheetsWorkbook::getLastOpenedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SheetsWorkbook::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<Long, SheetsWorkbookShare> toShareMap(List<SheetsWorkbookShare> shares) {
        Map<Long, SheetsWorkbookShare> result = new LinkedHashMap<>();
        for (SheetsWorkbookShare share : shares) {
            result.putIfAbsent(share.getWorkbookId(), share);
        }
        return result;
    }

    private Map<Long, UserAccount> loadOwnerMap(List<SheetsWorkbook> workbooks) {
        Set<Long> ownerIds = workbooks.stream().map(SheetsWorkbook::getOwnerId).collect(java.util.stream.Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, ownerIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, item -> item));
    }

    private Map<Long, Integer> loadCollaboratorCountMap(List<SheetsWorkbook> workbooks) {
        Set<Long> workbookIds = workbooks.stream().map(SheetsWorkbook::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (workbookIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        List<SheetsWorkbookShare> shares = sheetsWorkbookShareMapper.selectList(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .in(SheetsWorkbookShare::getWorkbookId, workbookIds)
                .ne(SheetsWorkbookShare::getResponseStatus, SHARE_STATUS_DECLINED));
        for (SheetsWorkbookShare share : shares) {
            result.merge(share.getWorkbookId(), 1, Integer::sum);
        }
        return result;
    }

    private int countCollaborators(Long workbookId) {
        Long count = sheetsWorkbookShareMapper.selectCount(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getWorkbookId, workbookId)
                .ne(SheetsWorkbookShare::getResponseStatus, SHARE_STATUS_DECLINED));
        return count == null ? 0 : count.intValue();
    }

    private SheetsWorkbook loadWorkbook(Long workbookId) {
        SheetsWorkbook workbook = sheetsWorkbookMapper.selectById(workbookId);
        if (workbook == null) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_NOT_FOUND);
        }
        return workbook;
    }

    private UserAccount loadUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private SheetsWorkbookAccessContext toContext(
            SheetsWorkbook workbook,
            UserAccount owner,
            SheetsWorkbookShare share,
            int collaboratorCount
    ) {
        if (share == null) {
            return new SheetsWorkbookAccessContext(workbook, owner, PERMISSION_OWNER, false, collaboratorCount);
        }
        return new SheetsWorkbookAccessContext(workbook, owner, share.getPermission(), true, collaboratorCount);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    public record SheetsWorkbookAccessContext(
            SheetsWorkbook workbook,
            UserAccount owner,
            String permission,
            boolean shared,
            int collaboratorCount
    ) {

        public boolean canEdit() {
            return PERMISSION_OWNER.equals(permission) || PERMISSION_EDIT.equals(permission);
        }

        public boolean isOwner() {
            return PERMISSION_OWNER.equals(permission);
        }

        public String scope() {
            return shared ? SCOPE_SHARED : SCOPE_OWNED;
        }
    }
}
