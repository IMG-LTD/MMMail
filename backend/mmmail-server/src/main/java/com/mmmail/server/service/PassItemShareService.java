package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgPolicyMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.mapper.PassItemShareMapper;
import com.mmmail.server.mapper.PassSecureLinkMapper;
import com.mmmail.server.mapper.PassSharedVaultMapper;
import com.mmmail.server.mapper.PassSharedVaultMemberMapper;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreatePassItemShareRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.PassItemShare;
import com.mmmail.server.model.entity.PassSecureLink;
import com.mmmail.server.model.entity.PassSharedVault;
import com.mmmail.server.model.entity.PassSharedVaultMember;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.PassIncomingSharedItemDetailVo;
import com.mmmail.server.model.vo.PassIncomingSharedItemSummaryVo;
import com.mmmail.server.model.vo.PassItemShareVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PassItemShareService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgPolicyMapper orgPolicyMapper;
    private final PassVaultItemMapper passVaultItemMapper;
    private final PassSharedVaultMapper passSharedVaultMapper;
    private final PassSharedVaultMemberMapper passSharedVaultMemberMapper;
    private final PassItemShareMapper passItemShareMapper;
    private final PassSecureLinkMapper passSecureLinkMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;

    public PassItemShareService(
            OrgWorkspaceMapper orgWorkspaceMapper,
            OrgMemberMapper orgMemberMapper,
            OrgPolicyMapper orgPolicyMapper,
            PassVaultItemMapper passVaultItemMapper,
            PassSharedVaultMapper passSharedVaultMapper,
            PassSharedVaultMemberMapper passSharedVaultMemberMapper,
            PassItemShareMapper passItemShareMapper,
            PassSecureLinkMapper passSecureLinkMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService
    ) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgPolicyMapper = orgPolicyMapper;
        this.passVaultItemMapper = passVaultItemMapper;
        this.passSharedVaultMapper = passSharedVaultMapper;
        this.passSharedVaultMemberMapper = passSharedVaultMemberMapper;
        this.passItemShareMapper = passItemShareMapper;
        this.passSecureLinkMapper = passSecureLinkMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
    }

    public List<PassItemShareVo> listItemShares(Long userId, Long orgId, Long itemId, String ipAddress) {
        PassVaultItem item = requireSourceItemAccess(userId, orgId, itemId);
        List<PassItemShare> shares = passItemShareMapper.selectList(new LambdaQueryWrapper<PassItemShare>()
                .eq(PassItemShare::getOrgId, orgId)
                .eq(PassItemShare::getItemId, itemId)
                .orderByAsc(PassItemShare::getCollaboratorEmail));
        Map<Long, String> actorEmails = loadUserEmailMap(shares.stream()
                .map(PassItemShare::getCreatedBy)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        auditService.record(userId, "PASS_ITEM_SHARE_LIST", "orgId=" + orgId + ",itemId=" + item.getId() + ",count=" + shares.size(), ipAddress, orgId);
        return shares.stream().map(share -> toItemShareVo(share, actorEmails)).toList();
    }

    @Transactional
    public PassItemShareVo createItemShare(
            Long userId,
            Long orgId,
            Long itemId,
            CreatePassItemShareRequest request,
            String ipAddress
    ) {
        if (!allowItemSharing(orgId)) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Pass policy does not allow individual item sharing");
        }
        PassVaultItem item = requireSourceItemAccess(userId, orgId, itemId);
        OrgMember targetMember = requireTargetMember(orgId, normalizeEmail(request.email()));
        if (targetMember.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "You already have access to this item");
        }
        LocalDateTime now = LocalDateTime.now();
        PassItemShare share = passItemShareMapper.selectOne(new LambdaQueryWrapper<PassItemShare>()
                .eq(PassItemShare::getItemId, itemId)
                .eq(PassItemShare::getCollaboratorUserId, targetMember.getUserId()));
        if (share == null) {
            share = new PassItemShare();
            share.setOrgId(orgId);
            share.setItemId(itemId);
            share.setSharedVaultId(item.getSharedVaultId());
            share.setOwnerId(item.getOwnerId());
            share.setCollaboratorUserId(targetMember.getUserId());
            share.setCollaboratorEmail(targetMember.getUserEmail());
            share.setCreatedBy(userId);
            share.setCreatedAt(now);
            share.setUpdatedAt(now);
            share.setDeleted(0);
            passItemShareMapper.insert(share);
        } else {
            share.setCollaboratorEmail(targetMember.getUserEmail());
            share.setCreatedBy(userId);
            share.setUpdatedAt(now);
            share.setDeleted(0);
            passItemShareMapper.updateById(share);
        }
        auditService.record(
                userId,
                "PASS_ITEM_SHARE_CREATE",
                "orgId=" + orgId + ",itemId=" + itemId + ",target=" + targetMember.getUserEmail(),
                ipAddress,
                orgId
        );
        return toItemShareVo(share, Map.of(userId, resolveUserEmail(userId)));
    }

    @Transactional
    public void removeItemShare(Long userId, Long orgId, Long itemId, Long shareId, String ipAddress) {
        requireSourceItemAccess(userId, orgId, itemId);
        PassItemShare share = passItemShareMapper.selectById(shareId);
        if (share == null || !orgId.equals(share.getOrgId()) || !itemId.equals(share.getItemId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item share is not found");
        }
        passItemShareMapper.deleteById(shareId);
        auditService.record(
                userId,
                "PASS_ITEM_SHARE_REMOVE",
                "orgId=" + orgId + ",itemId=" + itemId + ",target=" + share.getCollaboratorEmail(),
                ipAddress,
                orgId
        );
    }

    public List<PassIncomingSharedItemSummaryVo> listIncomingSharedItems(
            Long userId,
            Long orgId,
            String keyword,
            Boolean favoriteOnly,
            Integer limit,
            String itemType,
            String ipAddress
    ) {
        requireActiveOrgMembership(userId, orgId);
        List<PassItemShare> shares = passItemShareMapper.selectList(new LambdaQueryWrapper<PassItemShare>()
                .eq(PassItemShare::getOrgId, orgId)
                .eq(PassItemShare::getCollaboratorUserId, userId)
                .orderByDesc(PassItemShare::getUpdatedAt));
        List<PassIncomingSharedItemSummaryVo> result = toIncomingItemSummaries(shares, keyword, favoriteOnly, limit, itemType);
        auditService.record(userId, "PASS_INCOMING_ITEM_SHARE_LIST", "orgId=" + orgId + ",count=" + result.size(), ipAddress, orgId);
        return result;
    }

    public PassIncomingSharedItemDetailVo getIncomingSharedItem(Long userId, Long orgId, Long itemId, String ipAddress) {
        requireActiveOrgMembership(userId, orgId);
        PassItemShare share = passItemShareMapper.selectOne(new LambdaQueryWrapper<PassItemShare>()
                .eq(PassItemShare::getOrgId, orgId)
                .eq(PassItemShare::getCollaboratorUserId, userId)
                .eq(PassItemShare::getItemId, itemId));
        if (share == null) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No incoming item share access");
        }
        PassVaultItem item = passVaultItemMapper.selectById(itemId);
        PassSharedVault vault = passSharedVaultMapper.selectById(share.getSharedVaultId());
        if (item == null || vault == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared item is no longer available");
        }
        auditService.record(userId, "PASS_INCOMING_ITEM_SHARE_GET", "orgId=" + orgId + ",itemId=" + itemId, ipAddress, orgId);
        return toIncomingDetailVo(share, item, vault, resolveUserEmail(item.getOwnerId()));
    }

    private List<PassIncomingSharedItemSummaryVo> toIncomingItemSummaries(
            List<PassItemShare> shares,
            String keyword,
            Boolean favoriteOnly,
            Integer limit,
            String itemType
    ) {
        if (shares.isEmpty()) {
            return List.of();
        }
        Map<Long, PassItemShare> shareMap = shares.stream()
                .collect(Collectors.toMap(PassItemShare::getItemId, share -> share, (left, right) -> left, LinkedHashMap::new));
        List<PassVaultItem> items = loadIncomingItems(shareMap.keySet());
        Map<Long, PassSharedVault> vaultMap = loadVaultMap(items);
        Map<Long, String> ownerEmails = loadUserEmailMap(items.stream()
                .map(PassVaultItem::getOwnerId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedItemType = normalizeItemType(itemType);
        int safeLimit = safeLimit(limit);
        List<PassIncomingSharedItemSummaryVo> result = new ArrayList<>();
        for (PassVaultItem item : items) {
            if (!matchesIncomingFilters(item, normalizedKeyword, favoriteOnly, normalizedItemType)) {
                continue;
            }
            PassItemShare share = shareMap.get(item.getId());
            PassSharedVault vault = vaultMap.get(item.getSharedVaultId());
            if (share == null || vault == null) {
                continue;
            }
            result.add(toIncomingSummaryVo(share, item, vault, ownerEmails.get(item.getOwnerId())));
            if (result.size() >= safeLimit) {
                break;
            }
        }
        return result;
    }

    private List<PassVaultItem> loadIncomingItems(Set<Long> itemIds) {
        return passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .in(PassVaultItem::getId, itemIds)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED)
                .orderByDesc(PassVaultItem::getUpdatedAt));
    }

    private Map<Long, PassSharedVault> loadVaultMap(List<PassVaultItem> items) {
        Set<Long> vaultIds = items.stream()
                .map(PassVaultItem::getSharedVaultId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (vaultIds.isEmpty()) {
            return Map.of();
        }
        return passSharedVaultMapper.selectList(new LambdaQueryWrapper<PassSharedVault>()
                        .in(PassSharedVault::getId, vaultIds))
                .stream()
                .collect(Collectors.toMap(PassSharedVault::getId, vault -> vault));
    }

    private PassVaultItem requireSourceItemAccess(Long userId, Long orgId, Long itemId) {
        requireActiveOrgMembership(userId, orgId);
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED));
        if (item == null || item.getSharedVaultId() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared item is not found");
        }
        ensureVaultAccess(userId, orgId, item.getSharedVaultId());
        return item;
    }

    private void ensureVaultAccess(Long userId, Long orgId, Long vaultId) {
        OrgMember member = requireActiveOrgMembership(userId, orgId);
        PassSharedVault vault = passSharedVaultMapper.selectById(vaultId);
        if (vault == null || !orgId.equals(vault.getOrgId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault is not found");
        }
        if (isOrgManager(member.getRole())) {
            return;
        }
        PassSharedVaultMember directMember = passSharedVaultMemberMapper.selectOne(new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getVaultId, vaultId)
                .eq(PassSharedVaultMember::getUserId, userId));
        if (directMember == null) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this shared vault");
        }
    }

    private OrgMember requireTargetMember(Long orgId, String email) {
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getStatus, PassBusinessConstants.ORG_STATUS_ACTIVE)
                .eq(OrgMember::getUserEmail, email));
        if (member == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Organization member is not found");
        }
        return member;
    }

    private OrgMember requireActiveOrgMembership(Long userId, Long orgId) {
        OrgWorkspace org = orgWorkspaceMapper.selectById(orgId);
        if (org == null) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND);
        }
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId));
        if (member == null || !PassBusinessConstants.ORG_STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this organization");
        }
        return member;
    }

    private boolean allowItemSharing(Long orgId) {
        OrgPolicy policy = orgPolicyMapper.selectOne(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .eq(OrgPolicy::getPolicyKey, PassBusinessConstants.POLICY_ALLOW_ITEM_SHARING)
                .orderByDesc(OrgPolicy::getUpdatedAt)
                .last("limit 1"));
        if (policy == null || !StringUtils.hasText(policy.getPolicyValue())) {
            return PassBusinessConstants.DEFAULT_ALLOW_ITEM_SHARING;
        }
        return Boolean.parseBoolean(policy.getPolicyValue().trim());
    }

    private boolean matchesIncomingFilters(
            PassVaultItem item,
            String keyword,
            Boolean favoriteOnly,
            String itemType
    ) {
        if (Boolean.TRUE.equals(favoriteOnly) && (item.getFavorite() == null || item.getFavorite() != 1)) {
            return false;
        }
        if (itemType != null && !itemType.equals(item.getItemType())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(item.getTitle(), keyword)
                || containsIgnoreCase(item.getWebsite(), keyword)
                || containsIgnoreCase(item.getUsername(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeItemType(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            return null;
        }
        String normalized = itemType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                PassBusinessConstants.ITEM_TYPE_LOGIN,
                PassBusinessConstants.ITEM_TYPE_PASSWORD,
                PassBusinessConstants.ITEM_TYPE_NOTE,
                PassBusinessConstants.ITEM_TYPE_CARD,
                PassBusinessConstants.ITEM_TYPE_ALIAS,
                PassBusinessConstants.ITEM_TYPE_PASSKEY
        ).contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported pass item type");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Collaborator email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private boolean isOrgManager(String role) {
        return PassBusinessConstants.ORG_ROLE_OWNER.equals(role) || PassBusinessConstants.ORG_ROLE_ADMIN.equals(role);
    }

    private PassItemShareVo toItemShareVo(PassItemShare share, Map<Long, String> actorEmails) {
        return new PassItemShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getItemId()),
                String.valueOf(share.getCollaboratorUserId()),
                share.getCollaboratorEmail(),
                actorEmails.get(share.getCreatedBy()),
                share.getCreatedAt(),
                share.getUpdatedAt()
        );
    }

    private PassIncomingSharedItemSummaryVo toIncomingSummaryVo(
            PassItemShare share,
            PassVaultItem item,
            PassSharedVault vault,
            String ownerEmail
    ) {
        return new PassIncomingSharedItemSummaryVo(
                String.valueOf(share.getId()),
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getItemType(),
                String.valueOf(vault.getId()),
                vault.getName(),
                ownerEmail,
                item.getUpdatedAt(),
                true
        );
    }

    private PassIncomingSharedItemDetailVo toIncomingDetailVo(
            PassItemShare share,
            PassVaultItem item,
            PassSharedVault vault,
            String ownerEmail
    ) {
        return new PassIncomingSharedItemDetailVo(
                String.valueOf(share.getId()),
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getSecretCiphertext(),
                item.getNote(),
                item.getItemType(),
                String.valueOf(vault.getId()),
                vault.getName(),
                ownerEmail,
                item.getCreatedAt(),
                item.getUpdatedAt(),
                true
        );
    }

    private Map<Long, String> loadUserEmailMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getEmail));
    }

    private String resolveUserEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        UserAccount user = userAccountMapper.selectById(userId);
        return user == null ? null : user.getEmail();
    }
}
