package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.mapper.SheetsWorkbookShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateSheetsWorkbookShareRequest;
import com.mmmail.server.model.dto.RespondSheetsWorkbookShareRequest;
import com.mmmail.server.model.dto.UpdateSheetsWorkbookShareRequest;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.entity.SheetsWorkbookShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SheetsIncomingShareVo;
import com.mmmail.server.model.vo.SheetsWorkbookShareVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SheetsSharingService {

    private static final String STATUS_NEEDS_ACTION = "NEEDS_ACTION";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_DECLINED = "DECLINED";

    private final SheetsAccessService sheetsAccessService;
    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final SheetsWorkbookShareMapper sheetsWorkbookShareMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;

    public SheetsSharingService(
            SheetsAccessService sheetsAccessService,
            SheetsWorkbookMapper sheetsWorkbookMapper,
            SheetsWorkbookShareMapper sheetsWorkbookShareMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.sheetsAccessService = sheetsAccessService;
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.sheetsWorkbookShareMapper = sheetsWorkbookShareMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    @Transactional
    public SheetsWorkbookShareVo createShare(
            Long userId,
            Long workbookId,
            CreateSheetsWorkbookShareRequest request,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        UserAccount collaborator = loadCollaborator(request.targetEmail());
        ensureNotSelf(context.workbook().getOwnerId(), collaborator.getId());
        sheetsWorkbookShareMapper.purgeByWorkbookAndCollaborator(workbookId, collaborator.getId());
        SheetsWorkbookShare share = insertShare(context.workbook(), collaborator, normalizePermission(request.permission()));
        publishShareEvent(
                userId,
                collaborator.getId(),
                "SHEETS_WORKBOOK_SHARE_ADD",
                context.workbook(),
                ipAddress,
                "shareId=" + share.getId() + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        );
        return toShareVo(share, collaborator);
    }

    public List<SheetsWorkbookShareVo> listShares(Long userId, Long workbookId) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        List<SheetsWorkbookShare> shares = sheetsWorkbookShareMapper.selectList(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getOwnerId, userId)
                .eq(SheetsWorkbookShare::getWorkbookId, workbookId)
                .orderByDesc(SheetsWorkbookShare::getUpdatedAt)
                .orderByAsc(SheetsWorkbookShare::getCollaboratorUserId));
        return toShareVos(shares, loadUserMap(shares), context.workbook());
    }

    @Transactional
    public SheetsWorkbookShareVo updateSharePermission(
            Long userId,
            Long workbookId,
            Long shareId,
            UpdateSheetsWorkbookShareRequest request,
            String ipAddress
    ) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbookShare share = loadShareOwnedByWorkbook(userId, workbookId, shareId);
        share.setPermission(normalizePermission(request.permission()));
        share.setUpdatedAt(LocalDateTime.now());
        sheetsWorkbookShareMapper.updateById(share);
        UserAccount collaborator = loadUser(share.getCollaboratorUserId());
        publishShareEvent(
                userId,
                collaborator.getId(),
                "SHEETS_WORKBOOK_SHARE_PERMISSION_UPDATE",
                context.workbook(),
                ipAddress,
                "shareId=" + share.getId() + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        );
        return toShareVo(share, collaborator);
    }

    @Transactional
    public void removeShare(Long userId, Long workbookId, Long shareId, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbookShare share = loadShareOwnedByWorkbook(userId, workbookId, shareId);
        UserAccount collaborator = loadUser(share.getCollaboratorUserId());
        int affected = sheetsWorkbookShareMapper.purgeByOwnerWorkbookAndId(userId, workbookId, shareId);
        if (affected == 0) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_SHARE_NOT_FOUND);
        }
        publishShareEvent(
                userId,
                collaborator.getId(),
                "SHEETS_WORKBOOK_SHARE_REVOKE",
                context.workbook(),
                ipAddress,
                "shareId=" + shareId + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission()
        );
    }

    public List<SheetsIncomingShareVo> listIncomingShares(Long userId, String ipAddress) {
        List<SheetsWorkbookShare> shares = sheetsWorkbookShareMapper.selectList(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getCollaboratorUserId, userId)
                .orderByDesc(SheetsWorkbookShare::getUpdatedAt));
        List<SheetsIncomingShareVo> result = buildIncomingShareVos(shares, loadWorkbookMap(shares), loadUserMapByOwner(shares));
        auditService.record(userId, "SHEETS_WORKBOOK_INCOMING_QUERY", "count=" + result.size(), ipAddress);
        return result;
    }

    @Transactional
    public SheetsIncomingShareVo respondShare(
            Long userId,
            Long shareId,
            RespondSheetsWorkbookShareRequest request,
            String ipAddress
    ) {
        SheetsWorkbookShare share = sheetsWorkbookShareMapper.selectOne(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getId, shareId)
                .eq(SheetsWorkbookShare::getCollaboratorUserId, userId)
                .last("limit 1"));
        if (share == null) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_SHARE_NOT_FOUND);
        }
        share.setResponseStatus(normalizeResponse(request.response()));
        share.setUpdatedAt(LocalDateTime.now());
        sheetsWorkbookShareMapper.updateById(share);
        SheetsWorkbook workbook = sheetsWorkbookMapper.selectById(share.getWorkbookId());
        UserAccount owner = loadUser(share.getOwnerId());
        publishShareEvent(
                userId,
                owner.getId(),
                STATUS_ACCEPTED.equals(share.getResponseStatus())
                        ? "SHEETS_WORKBOOK_SHARE_ACCEPT"
                        : "SHEETS_WORKBOOK_SHARE_DECLINE",
                workbook,
                ipAddress,
                "shareId=" + share.getId() + ",ownerEmail=" + owner.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        );
        return toIncomingShareVo(share, workbook, owner);
    }

    @Transactional
    public void purgeByWorkbookId(Long workbookId) {
        sheetsWorkbookShareMapper.purgeByWorkbookId(workbookId);
    }

    private SheetsWorkbookShare insertShare(SheetsWorkbook workbook, UserAccount collaborator, String permission) {
        LocalDateTime now = LocalDateTime.now();
        SheetsWorkbookShare share = new SheetsWorkbookShare();
        share.setWorkbookId(workbook.getId());
        share.setOwnerId(workbook.getOwnerId());
        share.setCollaboratorUserId(collaborator.getId());
        share.setPermission(permission);
        share.setResponseStatus(STATUS_NEEDS_ACTION);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);
        share.setDeleted(0);
        sheetsWorkbookShareMapper.insert(share);
        return share;
    }

    private SheetsWorkbookShare loadShareOwnedByWorkbook(Long userId, Long workbookId, Long shareId) {
        SheetsWorkbookShare share = sheetsWorkbookShareMapper.selectOne(new LambdaQueryWrapper<SheetsWorkbookShare>()
                .eq(SheetsWorkbookShare::getOwnerId, userId)
                .eq(SheetsWorkbookShare::getWorkbookId, workbookId)
                .eq(SheetsWorkbookShare::getId, shareId)
                .last("limit 1"));
        if (share == null) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_SHARE_NOT_FOUND);
        }
        return share;
    }

    private UserAccount loadCollaborator(String targetEmail) {
        String normalizedEmail = normalizeEmail(targetEmail);
        UserAccount collaborator = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, normalizedEmail)
                .last("limit 1"));
        if (collaborator == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "Collaborator email is not registered");
        }
        return collaborator;
    }

    private UserAccount loadUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private Map<Long, UserAccount> loadUserMap(List<SheetsWorkbookShare> shares) {
        Set<Long> userIds = shares.stream().map(SheetsWorkbookShare::getCollaboratorUserId).collect(java.util.stream.Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, item -> item));
    }

    private Map<Long, UserAccount> loadUserMapByOwner(List<SheetsWorkbookShare> shares) {
        Set<Long> ownerIds = shares.stream().map(SheetsWorkbookShare::getOwnerId).collect(java.util.stream.Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, ownerIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, item -> item));
    }

    private Map<Long, SheetsWorkbook> loadWorkbookMap(List<SheetsWorkbookShare> shares) {
        Set<Long> workbookIds = shares.stream().map(SheetsWorkbookShare::getWorkbookId).collect(java.util.stream.Collectors.toSet());
        if (workbookIds.isEmpty()) {
            return Map.of();
        }
        return sheetsWorkbookMapper.selectList(new LambdaQueryWrapper<SheetsWorkbook>().in(SheetsWorkbook::getId, workbookIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(SheetsWorkbook::getId, item -> item));
    }

    private List<SheetsWorkbookShareVo> toShareVos(
            List<SheetsWorkbookShare> shares,
            Map<Long, UserAccount> userMap,
            SheetsWorkbook workbook
    ) {
        List<SheetsWorkbookShareVo> result = new ArrayList<>(shares.size());
        for (SheetsWorkbookShare share : shares) {
            result.add(toShareVo(share, userMap.get(share.getCollaboratorUserId())));
        }
        return result;
    }

    private SheetsWorkbookShareVo toShareVo(SheetsWorkbookShare share, UserAccount collaborator) {
        return new SheetsWorkbookShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getCollaboratorUserId()),
                collaborator == null ? "" : collaborator.getEmail(),
                collaborator == null ? "" : collaborator.getDisplayName(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getCreatedAt(),
                share.getUpdatedAt()
        );
    }

    private List<SheetsIncomingShareVo> buildIncomingShareVos(
            List<SheetsWorkbookShare> shares,
            Map<Long, SheetsWorkbook> workbookMap,
            Map<Long, UserAccount> ownerMap
    ) {
        List<SheetsIncomingShareVo> result = new ArrayList<>(shares.size());
        for (SheetsWorkbookShare share : shares) {
            result.add(toIncomingShareVo(share, workbookMap.get(share.getWorkbookId()), ownerMap.get(share.getOwnerId())));
        }
        return result;
    }

    private SheetsIncomingShareVo toIncomingShareVo(
            SheetsWorkbookShare share,
            SheetsWorkbook workbook,
            UserAccount owner
    ) {
        return new SheetsIncomingShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getWorkbookId()),
                workbook == null ? "(workbook deleted)" : workbook.getTitle(),
                owner == null ? "" : owner.getEmail(),
                owner == null ? "" : owner.getDisplayName(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getUpdatedAt()
        );
    }

    private void publishShareEvent(
            Long actorId,
            Long recipientId,
            String eventType,
            SheetsWorkbook workbook,
            String ipAddress,
            String extraDetail
    ) {
        AuditEventVo event = auditService.recordEvent(
                actorId,
                eventType,
                buildAuditDetail(workbook, extraDetail),
                ipAddress
        );
        suiteCollaborationService.publishToUser(actorId, event);
        if (recipientId != null && !recipientId.equals(actorId)) {
            suiteCollaborationService.publishToUser(recipientId, event);
        }
    }

    private String buildAuditDetail(SheetsWorkbook workbook, String extraDetail) {
        if (workbook == null) {
            return extraDetail;
        }
        String detail = "workbookId=" + workbook.getId()
                + ",title=" + workbook.getTitle()
                + ",version=" + workbook.getCurrentVersion()
                + ",activeSheetId=" + workbook.getActiveSheetId();
        if (StringUtils.hasText(extraDetail)) {
            return detail + "," + extraDetail.trim();
        }
        return detail;
    }

    private void ensureNotSelf(Long ownerId, Long collaboratorUserId) {
        if (ownerId.equals(collaboratorUserId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot share workbook with yourself");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePermission(String permission) {
        return StringUtils.hasText(permission) ? permission.trim().toUpperCase() : SheetsAccessService.PERMISSION_VIEW;
    }

    private String normalizeResponse(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Response is required");
        }
        return switch (response.trim().toUpperCase()) {
            case "ACCEPT" -> STATUS_ACCEPTED;
            case "DECLINE" -> STATUS_DECLINED;
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported share response");
        };
    }
}
