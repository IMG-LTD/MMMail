package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgPolicyMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.mapper.PassSecureLinkMapper;
import com.mmmail.server.mapper.PassSharedVaultMapper;
import com.mmmail.server.mapper.PassSharedVaultMemberMapper;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.AddPassSharedVaultMemberRequest;
import com.mmmail.server.model.dto.CreatePassItemRequest;
import com.mmmail.server.model.dto.CreatePassSecureLinkRequest;
import com.mmmail.server.model.dto.CreatePassSharedVaultRequest;
import com.mmmail.server.model.dto.UpsertPassItemTwoFactorRequest;
import com.mmmail.server.model.dto.UpdatePassBusinessPolicyRequest;
import com.mmmail.server.model.dto.UpdatePassItemRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.PassSecureLink;
import com.mmmail.server.model.entity.PassSharedVault;
import com.mmmail.server.model.entity.PassSharedVaultMember;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.PassBusinessOverviewVo;
import com.mmmail.server.model.vo.PassBusinessPolicyVo;
import com.mmmail.server.model.vo.PassItemDetailVo;
import com.mmmail.server.model.vo.PassItemSummaryVo;
import com.mmmail.server.model.vo.PassPublicSecureLinkVo;
import com.mmmail.server.model.vo.PassSecureLinkDashboardVo;
import com.mmmail.server.model.vo.PassSecureLinkVo;
import com.mmmail.server.model.vo.PassSharedVaultMemberVo;
import com.mmmail.server.model.vo.PassSharedVaultSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PassBusinessService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_SECURE_LINK_VIEWS = 10;
    private static final int DEFAULT_SECURE_LINK_EXPIRES_DAYS = 7;
    private static final int MAX_SECURE_LINK_EXPIRES_DAYS = 30;
    private static final int MAX_ACTIVITY_SCAN = 200;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgPolicyMapper orgPolicyMapper;
    private final PassSharedVaultMapper passSharedVaultMapper;
    private final PassSharedVaultMemberMapper passSharedVaultMemberMapper;
    private final PassVaultItemMapper passVaultItemMapper;
    private final PassSecureLinkMapper passSecureLinkMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final PassItemTwoFactorSupport passItemTwoFactorSupport;
    private final TotpCodeService totpCodeService;

    public PassBusinessService(
            OrgWorkspaceMapper orgWorkspaceMapper,
            OrgMemberMapper orgMemberMapper,
            OrgPolicyMapper orgPolicyMapper,
            PassSharedVaultMapper passSharedVaultMapper,
            PassSharedVaultMemberMapper passSharedVaultMemberMapper,
            PassVaultItemMapper passVaultItemMapper,
            PassSecureLinkMapper passSecureLinkMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            PassItemTwoFactorSupport passItemTwoFactorSupport,
            TotpCodeService totpCodeService
    ) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgPolicyMapper = orgPolicyMapper;
        this.passSharedVaultMapper = passSharedVaultMapper;
        this.passSharedVaultMemberMapper = passSharedVaultMemberMapper;
        this.passVaultItemMapper = passVaultItemMapper;
        this.passSecureLinkMapper = passSecureLinkMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.passItemTwoFactorSupport = passItemTwoFactorSupport;
        this.totpCodeService = totpCodeService;
    }

    public PassBusinessOverviewVo getOverview(Long userId, Long orgId, String ipAddress) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        List<PassSharedVault> vaults = loadAccessibleVaults(userId, orgId, orgMember.getRole(), null);
        Set<Long> vaultIds = vaultIds(vaults);
        List<PassVaultItem> items = loadSharedItemsByVaultIds(orgId, vaultIds);
        List<PassSecureLink> secureLinks = loadSecureLinksByItems(itemIds(items));
        List<OrgAuditEventVo> activity = listPassActivityInternal(orgId, DEFAULT_LIMIT);
        auditService.record(userId, "PASS_BUSINESS_OVERVIEW", "orgId=" + orgId + ",vaults=" + vaults.size(), ipAddress, orgId);
        return new PassBusinessOverviewVo(
                String.valueOf(orgId),
                orgMember.getRole(),
                vaults.size(),
                countActiveOrgMembers(orgId),
                items.size(),
                countActiveSecureLinks(secureLinks),
                countWeakPasswordItems(items, policy),
                countItemType(items, PassBusinessConstants.ITEM_TYPE_PASSKEY),
                countItemType(items, PassBusinessConstants.ITEM_TYPE_ALIAS),
                policy.allowSecureLinks(),
                policy.allowExternalSharing(),
                policy.forceTwoFactor(),
                policy.allowPasskeys(),
                policy.allowAliases(),
                activity.isEmpty() ? null : activity.getFirst().createdAt(),
                policy.updatedAt()
        );
    }

    public List<PassSharedVaultSummaryVo> listSharedVaults(Long userId, Long orgId, String keyword, String ipAddress) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        List<PassSharedVault> vaults = loadAccessibleVaults(userId, orgId, orgMember.getRole(), keyword);
        Map<Long, Integer> memberCounts = countVaultMembers(vaultIds(vaults));
        Map<Long, Integer> itemCounts = countVaultItems(vaultIds(vaults));
        Map<Long, String> creatorEmails = loadUserEmailMap(vaults.stream().map(PassSharedVault::getCreatedBy).collect(Collectors.toSet()));
        Map<Long, String> accessRoleMap = buildVaultAccessRoleMap(userId, orgId, orgMember.getRole(), vaultIds(vaults));
        auditService.record(userId, "PASS_SHARED_VAULT_LIST", "orgId=" + orgId + ",count=" + vaults.size(), ipAddress, orgId);
        return vaults.stream()
                .map(vault -> toVaultSummaryVo(vault, accessRoleMap.get(vault.getId()), memberCounts, itemCounts, creatorEmails))
                .toList();
    }

    @Transactional
    public PassSharedVaultSummaryVo createSharedVault(
            Long userId,
            Long orgId,
            CreatePassSharedVaultRequest request,
            String ipAddress
    ) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        ensureVaultCreationAllowed(orgMember.getRole(), policy.allowMemberVaultCreation());
        LocalDateTime now = LocalDateTime.now();

        PassSharedVault vault = new PassSharedVault();
        vault.setOrgId(orgId);
        vault.setName(normalizeVaultName(request.name()));
        vault.setDescription(normalizeDescription(request.description()));
        vault.setCreatedBy(userId);
        vault.setCreatedAt(now);
        vault.setUpdatedAt(now);
        vault.setDeleted(0);
        passSharedVaultMapper.insert(vault);

        PassSharedVaultMember member = new PassSharedVaultMember();
        member.setOrgId(orgId);
        member.setVaultId(vault.getId());
        member.setUserId(userId);
        member.setUserEmail(resolveUserEmail(userId));
        member.setRole(PassBusinessConstants.VAULT_ROLE_MANAGER);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        member.setDeleted(0);
        passSharedVaultMemberMapper.insert(member);

        auditService.record(
                userId,
                "PASS_SHARED_VAULT_CREATE",
                "orgId=" + orgId + ",vaultId=" + vault.getId() + ",name=" + vault.getName(),
                ipAddress,
                orgId
        );
        return toVaultSummaryVo(vault, PassBusinessConstants.VAULT_ROLE_MANAGER, Map.of(vault.getId(), 1), Map.of(vault.getId(), 0), Map.of(userId, resolveUserEmail(userId)));
    }

    public List<PassSharedVaultMemberVo> listSharedVaultMembers(Long userId, Long orgId, Long vaultId, String ipAddress) {
        requireVaultAccess(userId, orgId, vaultId);
        List<PassSharedVaultMember> members = passSharedVaultMemberMapper.selectList(new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getOrgId, orgId)
                .eq(PassSharedVaultMember::getVaultId, vaultId)
                .orderByAsc(PassSharedVaultMember::getRole)
                .orderByAsc(PassSharedVaultMember::getUserEmail));
        auditService.record(userId, "PASS_SHARED_VAULT_MEMBER_LIST", "orgId=" + orgId + ",vaultId=" + vaultId + ",count=" + members.size(), ipAddress, orgId);
        return members.stream().map(this::toVaultMemberVo).toList();
    }

    @Transactional
    public PassSharedVaultMemberVo addSharedVaultMember(
            Long userId,
            Long orgId,
            Long vaultId,
            AddPassSharedVaultMemberRequest request,
            String ipAddress
    ) {
        VaultAccessContext access = requireManagerAccess(userId, orgId, vaultId);
        String targetEmail = normalizeEmail(request.email());
        OrgMember targetOrgMember = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getStatus, PassBusinessConstants.ORG_STATUS_ACTIVE)
                .eq(OrgMember::getUserEmail, targetEmail));
        if (targetOrgMember == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Organization member is not found");
        }
        String targetRole = normalizeVaultRole(request.role());
        PassSharedVaultMember existing = passSharedVaultMemberMapper.selectOne(new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getVaultId, vaultId)
                .eq(PassSharedVaultMember::getUserId, targetOrgMember.getUserId()));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new PassSharedVaultMember();
            existing.setOrgId(orgId);
            existing.setVaultId(vaultId);
            existing.setUserId(targetOrgMember.getUserId());
            existing.setUserEmail(targetOrgMember.getUserEmail());
            existing.setRole(targetRole);
            existing.setCreatedAt(now);
            existing.setUpdatedAt(now);
            existing.setDeleted(0);
            passSharedVaultMemberMapper.insert(existing);
        } else {
            existing.setRole(targetRole);
            existing.setUserEmail(targetOrgMember.getUserEmail());
            existing.setUpdatedAt(now);
            existing.setDeleted(0);
            passSharedVaultMemberMapper.updateById(existing);
        }
        auditService.record(
                userId,
                "PASS_SHARED_VAULT_MEMBER_ADD",
                "orgId=" + orgId + ",vaultId=" + vaultId + ",target=" + targetEmail + ",role=" + targetRole,
                ipAddress,
                orgId
        );
        return toVaultMemberVo(existing);
    }

    @Transactional
    public void removeSharedVaultMember(Long userId, Long orgId, Long vaultId, Long memberId, String ipAddress) {
        VaultAccessContext access = requireManagerAccess(userId, orgId, vaultId);
        PassSharedVaultMember member = passSharedVaultMemberMapper.selectById(memberId);
        if (member == null || !orgId.equals(member.getOrgId()) || !vaultId.equals(member.getVaultId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault member is not found");
        }
        passSharedVaultMemberMapper.deleteById(memberId);
        auditService.record(
                userId,
                "PASS_SHARED_VAULT_MEMBER_REMOVE",
                "orgId=" + orgId + ",vaultId=" + vaultId + ",target=" + member.getUserEmail(),
                ipAddress,
                orgId
        );
    }

    public List<PassItemSummaryVo> listSharedItems(
            Long userId,
            Long orgId,
            Long vaultId,
            String keyword,
            Boolean favoriteOnly,
            Integer limit,
            String itemType,
            String ipAddress
    ) {
        requireVaultAccess(userId, orgId, vaultId);
        int safeLimit = safeLimit(limit);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedItemType = normalizeItemType(itemType);
        LambdaQueryWrapper<PassVaultItem> query = new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getSharedVaultId, vaultId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED)
                .orderByDesc(PassVaultItem::getUpdatedAt)
                .last("limit " + safeLimit);
        if (Boolean.TRUE.equals(favoriteOnly)) {
            query.eq(PassVaultItem::getFavorite, 1);
        }
        if (normalizedItemType != null) {
            query.eq(PassVaultItem::getItemType, normalizedItemType);
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            query.and(wrapper -> wrapper
                    .like(PassVaultItem::getTitle, normalizedKeyword)
                    .or()
                    .like(PassVaultItem::getWebsite, normalizedKeyword)
                    .or()
                    .like(PassVaultItem::getUsername, normalizedKeyword));
        }
        List<PassVaultItem> items = passVaultItemMapper.selectList(query);
        Map<Long, Integer> secureLinkCounts = countActiveSecureLinksByItem(itemIds(items));
        auditService.record(userId, "PASS_SHARED_ITEM_LIST", "orgId=" + orgId + ",vaultId=" + vaultId + ",count=" + items.size(), ipAddress, orgId);
        return items.stream().map(item -> toSummaryVo(item, secureLinkCounts)).toList();
    }

    @Transactional
    public PassItemDetailVo createSharedItem(
            Long userId,
            Long orgId,
            Long vaultId,
            CreatePassItemRequest request,
            String ipAddress
    ) {
        requireVaultAccess(userId, orgId, vaultId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        String normalizedType = normalizeItemType(request.itemType());
        String normalizedSecret = validateSecretForItemType(normalizedType, request.secretCiphertext(), policy);
        LocalDateTime now = LocalDateTime.now();

        PassVaultItem item = new PassVaultItem();
        item.setOwnerId(userId);
        item.setOrgId(orgId);
        item.setSharedVaultId(vaultId);
        item.setScopeType(PassBusinessConstants.SCOPE_SHARED);
        item.setItemType(normalizedType == null ? PassBusinessConstants.ITEM_TYPE_LOGIN : normalizedType);
        item.setTitle(requireTitle(request.title()));
        item.setWebsite(normalizeWebsite(request.website()));
        item.setUsername(normalizeUsername(request.username()));
        item.setSecretCiphertext(normalizedSecret);
        clearTwoFactorIfUnsupported(item);
        item.setNote(normalizeNote(request.note()));
        item.setFavorite(0);
        item.setMonitorExcluded(0);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        passVaultItemMapper.insert(item);

        auditService.record(
                userId,
                "PASS_SHARED_ITEM_CREATE",
                "orgId=" + orgId + ",vaultId=" + vaultId + ",itemId=" + item.getId() + ",itemType=" + item.getItemType(),
                ipAddress,
                orgId
        );
        return toDetailVo(item, resolveUserEmail(userId), 0);
    }

    public PassItemDetailVo getSharedItem(Long userId, Long orgId, Long vaultId, Long itemId, String ipAddress) {
        requireVaultAccess(userId, orgId, vaultId);
        PassVaultItem item = loadSharedItem(orgId, vaultId, itemId);
        auditService.record(userId, "PASS_SHARED_ITEM_GET", "orgId=" + orgId + ",vaultId=" + vaultId + ",itemId=" + itemId, ipAddress, orgId);
        return toDetailVo(item, resolveUserEmail(item.getOwnerId()), countActiveSecureLinksByItem(item.getId()));
    }

    @Transactional
    public PassItemDetailVo updateSharedItem(
            Long userId,
            Long orgId,
            Long vaultId,
            Long itemId,
            UpdatePassItemRequest request,
            String ipAddress
    ) {
        requireVaultAccess(userId, orgId, vaultId);
        PassVaultItem item = loadSharedItem(orgId, vaultId, itemId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        String normalizedType = normalizeItemType(request.itemType());
        String normalizedSecret = validateSecretForItemType(normalizedType, request.secretCiphertext(), policy);
        item.setTitle(requireTitle(request.title()));
        item.setItemType(normalizedType == null ? item.getItemType() : normalizedType);
        item.setWebsite(normalizeWebsite(request.website()));
        item.setUsername(normalizeUsername(request.username()));
        item.setSecretCiphertext(normalizedSecret);
        clearTwoFactorIfUnsupported(item);
        item.setNote(normalizeNote(request.note()));
        item.setUpdatedAt(LocalDateTime.now());
        passVaultItemMapper.updateById(item);
        auditService.record(
                userId,
                "PASS_SHARED_ITEM_UPDATE",
                "orgId=" + orgId + ",vaultId=" + vaultId + ",itemId=" + itemId + ",itemType=" + item.getItemType(),
                ipAddress,
                orgId
        );
        return toDetailVo(item, resolveUserEmail(item.getOwnerId()), countActiveSecureLinksByItem(itemId));
    }

    @Transactional
    public void deleteSharedItem(Long userId, Long orgId, Long vaultId, Long itemId, String ipAddress) {
        requireVaultAccess(userId, orgId, vaultId);
        PassVaultItem item = loadSharedItem(orgId, vaultId, itemId);
        revokeSecureLinksForItem(itemId);
        passVaultItemMapper.deleteById(itemId);
        auditService.record(
                userId,
                "PASS_SHARED_ITEM_DELETE",
                "orgId=" + orgId + ",vaultId=" + vaultId + ",itemId=" + itemId + ",itemType=" + item.getItemType(),
                ipAddress,
                orgId
        );
    }

    @Transactional
    public PassItemDetailVo upsertSharedItemTwoFactor(
            Long userId,
            Long orgId,
            Long itemId,
            UpsertPassItemTwoFactorRequest request,
            String ipAddress
    ) {
        PassVaultItem item = requireManageableSharedItem(userId, orgId, itemId);
        ensureTwoFactorSupported(item);
        passItemTwoFactorSupport.apply(
                item,
                passItemTwoFactorSupport.normalize(
                        request.issuer(),
                        request.accountName(),
                        request.secretCiphertext(),
                        request.algorithm(),
                        request.digits(),
                        request.periodSeconds()
                ),
                LocalDateTime.now()
        );
        passVaultItemMapper.updateById(item);
        auditService.record(
                userId,
                "PASS_SHARED_ITEM_2FA_SAVE",
                "orgId=" + orgId + ",itemId=" + itemId,
                ipAddress,
                orgId
        );
        return toDetailVo(item, resolveUserEmail(item.getOwnerId()), countActiveSecureLinksByItem(itemId));
    }

    @Transactional
    public PassItemDetailVo deleteSharedItemTwoFactor(Long userId, Long orgId, Long itemId, String ipAddress) {
        PassVaultItem item = requireManageableSharedItem(userId, orgId, itemId);
        ensureTwoFactorSupported(item);
        passItemTwoFactorSupport.clear(item, LocalDateTime.now());
        passVaultItemMapper.updateById(item);
        auditService.record(
                userId,
                "PASS_SHARED_ITEM_2FA_DELETE",
                "orgId=" + orgId + ",itemId=" + itemId,
                ipAddress,
                orgId
        );
        return toDetailVo(item, resolveUserEmail(item.getOwnerId()), countActiveSecureLinksByItem(itemId));
    }

    public AuthenticatorCodeVo generateSharedItemTwoFactorCode(Long userId, Long orgId, Long itemId, String ipAddress) {
        PassVaultItem item = loadSharedItemForOrg(userId, orgId, itemId);
        ensureHasTwoFactor(item);
        AuthenticatorCodeVo code = totpCodeService.generateCode(
                item.getTwoFactorSecretCiphertext(),
                passItemTwoFactorSupport.toVo(item).algorithm(),
                passItemTwoFactorSupport.toVo(item).digits(),
                passItemTwoFactorSupport.toVo(item).periodSeconds()
        );
        auditService.record(
                userId,
                "PASS_SHARED_ITEM_2FA_CODE_GENERATE",
                "orgId=" + orgId + ",itemId=" + itemId + ",digits=" + code.digits() + ",periodSeconds=" + code.periodSeconds(),
                ipAddress,
                orgId
        );
        return code;
    }

    public PassBusinessPolicyVo getPolicy(Long userId, Long orgId, String ipAddress) {
        requireActiveOrgMembership(userId, orgId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        auditService.record(userId, "PASS_POLICY_GET", "orgId=" + orgId, ipAddress, orgId);
        return toPolicyVo(orgId, policy);
    }

    @Transactional
    public PassBusinessPolicyVo updatePolicy(
            Long userId,
            Long orgId,
            UpdatePassBusinessPolicyRequest request,
            String ipAddress
    ) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        if (!canManageOrgPolicy(orgMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER or ADMIN can update Pass policy");
        }
        PassPolicySnapshot current = loadPolicySnapshot(orgId);
        PassPolicySnapshot next = mergePolicySnapshot(current, request);
        savePolicySnapshot(orgId, userId, next);
        auditService.record(
                userId,
                "PASS_POLICY_UPDATE",
                "orgId=" + orgId
                        + ",minimumPasswordLength=" + next.minimumPasswordLength()
                        + ",maximumPasswordLength=" + next.maximumPasswordLength()
                        + ",allowMemorablePasswords=" + next.allowMemorablePasswords()
                        + ",allowSecureLinks=" + next.allowSecureLinks()
                        + ",allowExternalSharing=" + next.allowExternalSharing()
                        + ",allowItemSharing=" + next.allowItemSharing()
                        + ",allowMemberVaultCreation=" + next.allowMemberVaultCreation()
                        + ",allowPasskeys=" + next.allowPasskeys()
                        + ",allowAliases=" + next.allowAliases(),
                ipAddress,
                orgId
        );
        return toPolicyVo(orgId, next);
    }

    public List<OrgAuditEventVo> listActivity(Long userId, Long orgId, Integer limit, String ipAddress) {
        requireActiveOrgMembership(userId, orgId);
        int safeLimit = safeLimit(limit);
        List<OrgAuditEventVo> events = listPassActivityInternal(orgId, safeLimit);
        auditService.record(userId, "PASS_ACTIVITY_LIST", "orgId=" + orgId + ",count=" + events.size(), ipAddress, orgId);
        return events;
    }

    public List<PassSecureLinkVo> listSecureLinks(
            Long userId,
            Long orgId,
            Long itemId,
            String publicBaseUrl,
            String ipAddress
    ) {
        PassVaultItem item = loadSharedItemForOrg(userId, orgId, itemId);
        List<PassSecureLink> links = passSecureLinkMapper.selectList(new LambdaQueryWrapper<PassSecureLink>()
                .eq(PassSecureLink::getOrgId, orgId)
                .eq(PassSecureLink::getItemId, itemId)
                .orderByDesc(PassSecureLink::getCreatedAt));
        auditService.record(userId, "PASS_SECURE_LINK_LIST", "orgId=" + orgId + ",itemId=" + itemId + ",count=" + links.size(), ipAddress, orgId);
        return links.stream().map(link -> toSecureLinkVo(link, publicBaseUrl)).toList();
    }

    public List<PassSecureLinkDashboardVo> listOrgSecureLinks(
            Long userId,
            Long orgId,
            String publicBaseUrl,
            String ipAddress
    ) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        List<PassSharedVault> vaults = loadAccessibleVaults(userId, orgId, orgMember.getRole(), null);
        Set<Long> vaultIds = vaultIds(vaults);
        if (vaultIds.isEmpty()) {
            auditService.record(userId, "PASS_SECURE_LINK_DASHBOARD_LIST", "orgId=" + orgId + ",count=0", ipAddress, orgId);
            return List.of();
        }
        List<PassSecureLink> links = passSecureLinkMapper.selectList(new LambdaQueryWrapper<PassSecureLink>()
                .eq(PassSecureLink::getOrgId, orgId)
                .in(PassSecureLink::getSharedVaultId, vaultIds)
                .orderByDesc(PassSecureLink::getCreatedAt));
        Map<Long, PassVaultItem> itemMap = loadPassItemMap(secureLinkItemIds(links));
        Map<Long, PassSharedVault> vaultMap = vaults.stream()
                .collect(Collectors.toMap(PassSharedVault::getId, vault -> vault, (left, right) -> left, LinkedHashMap::new));
        auditService.record(userId, "PASS_SECURE_LINK_DASHBOARD_LIST", "orgId=" + orgId + ",count=" + links.size(), ipAddress, orgId);
        return links.stream()
                .map(link -> toSecureLinkDashboardVo(link, publicBaseUrl, itemMap.get(link.getItemId()), vaultMap.get(link.getSharedVaultId())))
                .toList();
    }

    @Transactional
    public PassSecureLinkVo createSecureLink(
            Long userId,
            Long orgId,
            Long itemId,
            CreatePassSecureLinkRequest request,
            String publicBaseUrl,
            String ipAddress
    ) {
        PassVaultItem item = loadSharedItemForOrg(userId, orgId, itemId);
        PassPolicySnapshot policy = loadPolicySnapshot(orgId);
        if (!policy.allowSecureLinks() || !policy.allowExternalSharing()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Pass policy does not allow secure links");
        }
        LocalDateTime expiresAt = validateExpiresAt(request.expiresAt());
        LocalDateTime now = LocalDateTime.now();
        PassSecureLink link = new PassSecureLink();
        link.setOrgId(orgId);
        link.setItemId(itemId);
        link.setSharedVaultId(item.getSharedVaultId());
        link.setToken(generateUniqueSecureToken());
        link.setMaxViews(request.maxViews() == null ? DEFAULT_SECURE_LINK_VIEWS : request.maxViews());
        link.setCurrentViews(0);
        link.setExpiresAt(expiresAt);
        link.setRevokedAt(null);
        link.setCreatedBy(userId);
        link.setCreatedAt(now);
        link.setUpdatedAt(now);
        link.setDeleted(0);
        passSecureLinkMapper.insert(link);
        auditService.record(
                userId,
                "PASS_SECURE_LINK_CREATE",
                "orgId=" + orgId + ",itemId=" + itemId + ",linkId=" + link.getId() + ",maxViews=" + link.getMaxViews(),
                ipAddress,
                orgId
        );
        return toSecureLinkVo(link, publicBaseUrl);
    }

    @Transactional
    public PassSecureLinkVo revokeSecureLink(Long userId, Long orgId, Long linkId, String publicBaseUrl, String ipAddress) {
        PassSecureLink link = passSecureLinkMapper.selectById(linkId);
        if (link == null || !orgId.equals(link.getOrgId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link is not found");
        }
        requireVaultAccess(userId, orgId, link.getSharedVaultId());
        if (link.getRevokedAt() == null) {
            link.setRevokedAt(LocalDateTime.now());
            link.setUpdatedAt(LocalDateTime.now());
            passSecureLinkMapper.updateById(link);
            auditService.record(
                    userId,
                    "PASS_SECURE_LINK_REVOKE",
                    "orgId=" + orgId + ",itemId=" + link.getItemId() + ",linkId=" + linkId,
                    ipAddress,
                    orgId
            );
        }
        return toSecureLinkVo(link, publicBaseUrl);
    }

    @Transactional
    public PassPublicSecureLinkVo getPublicSecureLink(String token, String ipAddress) {
        PassSecureLink link = passSecureLinkMapper.selectOne(new LambdaQueryWrapper<PassSecureLink>()
                .eq(PassSecureLink::getToken, token));
        if (link == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link is not found");
        }
        validateSecureLinkActive(link);
        PassVaultItem item = passVaultItemMapper.selectById(link.getItemId());
        PassSharedVault vault = passSharedVaultMapper.selectById(link.getSharedVaultId());
        if (item == null || vault == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link target is not found");
        }
        link.setCurrentViews(link.getCurrentViews() + 1);
        link.setUpdatedAt(LocalDateTime.now());
        passSecureLinkMapper.updateById(link);
        auditService.record(
                null,
                "PASS_SECURE_LINK_VIEW",
                "orgId=" + link.getOrgId() + ",itemId=" + link.getItemId() + ",linkId=" + link.getId() + ",token=" + token,
                ipAddress,
                link.getOrgId()
        );
        Integer remainingViews = Math.max(0, link.getMaxViews() - link.getCurrentViews());
        return new PassPublicSecureLinkVo(
                String.valueOf(item.getId()),
                nullableId(item.getSharedVaultId()),
                vault.getName(),
                item.getItemType(),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getSecretCiphertext(),
                item.getNote(),
                link.getMaxViews(),
                link.getCurrentViews(),
                remainingViews,
                link.getExpiresAt()
        );
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

    private VaultAccessContext requireVaultAccess(Long userId, Long orgId, Long vaultId) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        PassSharedVault vault = passSharedVaultMapper.selectById(vaultId);
        if (vault == null || !orgId.equals(vault.getOrgId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault is not found");
        }
        if (isOrgManager(orgMember.getRole())) {
            return new VaultAccessContext(orgMember, vault, PassBusinessConstants.VAULT_ROLE_MANAGER);
        }
        PassSharedVaultMember member = passSharedVaultMemberMapper.selectOne(new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getVaultId, vaultId)
                .eq(PassSharedVaultMember::getUserId, userId));
        if (member == null) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this shared vault");
        }
        return new VaultAccessContext(orgMember, vault, member.getRole());
    }

    private VaultAccessContext requireManagerAccess(Long userId, Long orgId, Long vaultId) {
        VaultAccessContext access = requireVaultAccess(userId, orgId, vaultId);
        if (!PassBusinessConstants.VAULT_ROLE_MANAGER.equals(access.accessRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Manager access is required");
        }
        return access;
    }

    private List<PassSharedVault> loadAccessibleVaults(Long userId, Long orgId, String orgRole, String keyword) {
        LambdaQueryWrapper<PassSharedVault> query = new LambdaQueryWrapper<PassSharedVault>()
                .eq(PassSharedVault::getOrgId, orgId)
                .orderByDesc(PassSharedVault::getUpdatedAt);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            query.and(wrapper -> wrapper.like(PassSharedVault::getName, normalizedKeyword)
                    .or()
                    .like(PassSharedVault::getDescription, normalizedKeyword));
        }
        if (isOrgManager(orgRole)) {
            return passSharedVaultMapper.selectList(query);
        }
        Set<Long> vaultIds = passSharedVaultMemberMapper.selectList(new LambdaQueryWrapper<PassSharedVaultMember>()
                        .eq(PassSharedVaultMember::getOrgId, orgId)
                        .eq(PassSharedVaultMember::getUserId, userId))
                .stream()
                .map(PassSharedVaultMember::getVaultId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (vaultIds.isEmpty()) {
            return List.of();
        }
        query.in(PassSharedVault::getId, vaultIds);
        return passSharedVaultMapper.selectList(query);
    }

    private List<PassVaultItem> loadSharedItemsByVaultIds(Long orgId, Set<Long> vaultIds) {
        if (vaultIds.isEmpty()) {
            return List.of();
        }
        return passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED)
                .in(PassVaultItem::getSharedVaultId, vaultIds));
    }

    private PassVaultItem loadSharedItem(Long orgId, Long vaultId, Long itemId) {
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getSharedVaultId, vaultId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault item is not found");
        }
        return item;
    }

    private PassVaultItem loadSharedItemForOrg(Long userId, Long orgId, Long itemId) {
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault item is not found");
        }
        requireVaultAccess(userId, orgId, item.getSharedVaultId());
        return item;
    }

    private PassVaultItem requireManageableSharedItem(Long userId, Long orgId, Long itemId) {
        PassVaultItem item = loadSharedItemForOrg(userId, orgId, itemId);
        requireManagerAccess(userId, orgId, item.getSharedVaultId());
        return item;
    }

    private Map<Long, Integer> countVaultMembers(Set<Long> vaultIds) {
        if (vaultIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (PassSharedVaultMember member : passSharedVaultMemberMapper.selectList(new LambdaQueryWrapper<PassSharedVaultMember>()
                .in(PassSharedVaultMember::getVaultId, vaultIds))) {
            result.merge(member.getVaultId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, Integer> countVaultItems(Set<Long> vaultIds) {
        if (vaultIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (PassVaultItem item : passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED)
                .in(PassVaultItem::getSharedVaultId, vaultIds))) {
            result.merge(item.getSharedVaultId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, String> buildVaultAccessRoleMap(Long userId, Long orgId, String orgRole, Set<Long> vaultIds) {
        if (vaultIds.isEmpty()) {
            return Map.of();
        }
        if (isOrgManager(orgRole)) {
            Map<Long, String> result = new LinkedHashMap<>();
            for (Long vaultId : vaultIds) {
                result.put(vaultId, PassBusinessConstants.VAULT_ROLE_MANAGER);
            }
            return result;
        }
        return passSharedVaultMemberMapper.selectList(new LambdaQueryWrapper<PassSharedVaultMember>()
                        .eq(PassSharedVaultMember::getOrgId, orgId)
                        .eq(PassSharedVaultMember::getUserId, userId)
                        .in(PassSharedVaultMember::getVaultId, vaultIds))
                .stream()
                .collect(Collectors.toMap(PassSharedVaultMember::getVaultId, PassSharedVaultMember::getRole));
    }

    private Set<Long> vaultIds(List<PassSharedVault> vaults) {
        return vaults.stream().map(PassSharedVault::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> itemIds(List<PassVaultItem> items) {
        return items.stream().map(PassVaultItem::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> secureLinkItemIds(List<PassSecureLink> links) {
        return links.stream().map(PassSecureLink::getItemId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<PassSecureLink> loadSecureLinksByItems(Set<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return passSecureLinkMapper.selectList(new LambdaQueryWrapper<PassSecureLink>()
                .in(PassSecureLink::getItemId, itemIds));
    }

    private Map<Long, Integer> countActiveSecureLinksByItem(Set<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (PassSecureLink link : loadSecureLinksByItems(itemIds)) {
            if (isSecureLinkActive(link)) {
                result.merge(link.getItemId(), 1, Integer::sum);
            }
        }
        return result;
    }

    private int countActiveSecureLinksByItem(Long itemId) {
        return countActiveSecureLinksByItem(Set.of(itemId)).getOrDefault(itemId, 0);
    }

    private int countActiveSecureLinks(List<PassSecureLink> links) {
        int count = 0;
        for (PassSecureLink link : links) {
            if (isSecureLinkActive(link)) {
                count++;
            }
        }
        return count;
    }

    private int countWeakPasswordItems(List<PassVaultItem> items, PassPolicySnapshot policy) {
        int count = 0;
        for (PassVaultItem item : items) {
            if (isWeakPasswordItem(item, policy)) {
                count++;
            }
        }
        return count;
    }

    private boolean isWeakPasswordItem(PassVaultItem item, PassPolicySnapshot policy) {
        if (!requiresPasswordPolicy(item.getItemType())) {
            return false;
        }
        String secret = item.getSecretCiphertext();
        if (!StringUtils.hasText(secret)) {
            return true;
        }
        if (secret.length() < policy.minimumPasswordLength()) {
            return true;
        }
        if (policy.requireUppercase() && !secret.chars().anyMatch(Character::isUpperCase)) {
            return true;
        }
        if (policy.requireDigits() && !secret.chars().anyMatch(Character::isDigit)) {
            return true;
        }
        return policy.requireSymbols() && secret.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch));
    }

    private void clearTwoFactorIfUnsupported(PassVaultItem item) {
        if (!passItemTwoFactorSupport.supports(item)) {
            passItemTwoFactorSupport.clear(item, item.getUpdatedAt() == null ? LocalDateTime.now() : item.getUpdatedAt());
        }
    }

    private void ensureTwoFactorSupported(PassVaultItem item) {
        if (!passItemTwoFactorSupport.supports(item)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item type does not support built-in 2FA");
        }
    }

    private void ensureHasTwoFactor(PassVaultItem item) {
        ensureTwoFactorSupported(item);
        if (!passItemTwoFactorSupport.hasTwoFactor(item)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item 2FA is not configured");
        }
    }

    private int countItemType(List<PassVaultItem> items, String itemType) {
        int count = 0;
        for (PassVaultItem item : items) {
            if (itemType.equals(item.getItemType())) {
                count++;
            }
        }
        return count;
    }

    private int countActiveOrgMembers(Long orgId) {
        Long count = orgMemberMapper.selectCount(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getStatus, PassBusinessConstants.ORG_STATUS_ACTIVE));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private List<OrgAuditEventVo> listPassActivityInternal(Long orgId, int limit) {
        return auditService.listByOrg(orgId, Math.min(MAX_ACTIVITY_SCAN, Math.max(limit * 4, limit)), null, null, null)
                .stream()
                .filter(event -> event.eventType() != null && event.eventType().startsWith("PASS_"))
                .limit(limit)
                .toList();
    }

    private void ensureVaultCreationAllowed(String orgRole, boolean allowMemberVaultCreation) {
        if (isOrgManager(orgRole)) {
            return;
        }
        if (!PassBusinessConstants.ORG_ROLE_MEMBER.equals(orgRole) || !allowMemberVaultCreation) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Current role cannot create shared vaults");
        }
    }

    private boolean canManageOrgPolicy(String orgRole) {
        return PassBusinessConstants.ORG_ROLE_OWNER.equals(orgRole)
                || PassBusinessConstants.ORG_ROLE_ADMIN.equals(orgRole);
    }

    private boolean isOrgManager(String orgRole) {
        return PassBusinessConstants.ORG_ROLE_OWNER.equals(orgRole)
                || PassBusinessConstants.ORG_ROLE_ADMIN.equals(orgRole);
    }

    private String normalizeVaultName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared vault name is required");
        }
        return name.trim();
    }

    private String normalizeDescription(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Member email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeVaultRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Vault member role is required");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(PassBusinessConstants.VAULT_ROLE_MANAGER, PassBusinessConstants.VAULT_ROLE_MEMBER).contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported shared vault role");
        }
        return normalized;
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

    private String validateSecretForItemType(String itemType, String secretCiphertext, PassPolicySnapshot policy) {
        String normalizedType = itemType == null ? PassBusinessConstants.ITEM_TYPE_LOGIN : itemType;
        if (PassBusinessConstants.ITEM_TYPE_ALIAS.equals(normalizedType) && !policy.allowAliases()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Alias items are disabled by Pass policy");
        }
        if (PassBusinessConstants.ITEM_TYPE_PASSKEY.equals(normalizedType) && !policy.allowPasskeys()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Passkey items are disabled by Pass policy");
        }
        String normalizedSecret = StringUtils.hasText(secretCiphertext) ? secretCiphertext : null;
        if (requiresSecret(normalizedType) && !StringUtils.hasText(normalizedSecret)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item secret is required");
        }
        if (requiresPasswordPolicy(normalizedType) && StringUtils.hasText(normalizedSecret)) {
            validatePasswordPolicy(normalizedSecret, policy);
        }
        return normalizedSecret;
    }

    private boolean requiresSecret(String itemType) {
        return Set.of(
                PassBusinessConstants.ITEM_TYPE_LOGIN,
                PassBusinessConstants.ITEM_TYPE_PASSWORD,
                PassBusinessConstants.ITEM_TYPE_CARD,
                PassBusinessConstants.ITEM_TYPE_PASSKEY
        ).contains(itemType);
    }

    private boolean requiresPasswordPolicy(String itemType) {
        return Set.of(
                PassBusinessConstants.ITEM_TYPE_LOGIN,
                PassBusinessConstants.ITEM_TYPE_PASSWORD
        ).contains(itemType);
    }

    private void validatePasswordPolicy(String secretCiphertext, PassPolicySnapshot policy) {
        if (secretCiphertext.length() < policy.minimumPasswordLength()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item secret is shorter than organization minimum");
        }
        if (policy.requireUppercase() && secretCiphertext.chars().noneMatch(Character::isUpperCase)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item secret must include uppercase characters");
        }
        if (policy.requireDigits() && secretCiphertext.chars().noneMatch(Character::isDigit)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item secret must include digits");
        }
        if (policy.requireSymbols() && secretCiphertext.chars().allMatch(Character::isLetterOrDigit)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item secret must include symbols");
        }
    }

    private PassPolicySnapshot loadPolicySnapshot(Long orgId) {
        List<OrgPolicy> items = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .in(OrgPolicy::getPolicyKey, passPolicyKeys())
                .orderByDesc(OrgPolicy::getUpdatedAt));
        Map<String, String> valueMap = new LinkedHashMap<>();
        LocalDateTime latestUpdatedAt = null;
        for (OrgPolicy item : items) {
            if (!valueMap.containsKey(item.getPolicyKey())) {
                valueMap.put(item.getPolicyKey(), item.getPolicyValue());
            }
            if (latestUpdatedAt == null || (item.getUpdatedAt() != null && item.getUpdatedAt().isAfter(latestUpdatedAt))) {
                latestUpdatedAt = item.getUpdatedAt();
            }
        }
        return new PassPolicySnapshot(
                parseInt(valueMap.get(PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH), PassBusinessConstants.DEFAULT_MINIMUM_PASSWORD_LENGTH),
                parseInt(valueMap.get(PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH), PassBusinessConstants.DEFAULT_MAXIMUM_PASSWORD_LENGTH),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_UPPERCASE), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_DIGITS), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_REQUIRE_SYMBOLS), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS), PassBusinessConstants.DEFAULT_ALLOW_MEMORABLE_PASSWORDS),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_EXTERNAL_SHARING), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_ITEM_SHARING), PassBusinessConstants.DEFAULT_ALLOW_ITEM_SHARING),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_SECURE_LINKS), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_MEMBER_VAULT_CREATION), false),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_EXPORT), false),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_FORCE_TWO_FACTOR), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_PASSKEYS), true),
                parseBoolean(valueMap.get(PassBusinessConstants.POLICY_ALLOW_ALIASES), true),
                latestUpdatedAt
        );
    }

    private PassPolicySnapshot mergePolicySnapshot(PassPolicySnapshot current, UpdatePassBusinessPolicyRequest request) {
        int minimumPasswordLength = request.minimumPasswordLength() == null
                ? current.minimumPasswordLength()
                : request.minimumPasswordLength();
        int maximumPasswordLength = request.maximumPasswordLength() == null
                ? current.maximumPasswordLength()
                : request.maximumPasswordLength();
        validatePasswordLengthRange(minimumPasswordLength, maximumPasswordLength);
        return new PassPolicySnapshot(
                minimumPasswordLength,
                maximumPasswordLength,
                boolDefault(request.requireUppercase(), current.requireUppercase()),
                boolDefault(request.requireDigits(), current.requireDigits()),
                boolDefault(request.requireSymbols(), current.requireSymbols()),
                boolDefault(request.allowMemorablePasswords(), current.allowMemorablePasswords()),
                boolDefault(request.allowExternalSharing(), current.allowExternalSharing()),
                boolDefault(request.allowItemSharing(), current.allowItemSharing()),
                boolDefault(request.allowSecureLinks(), current.allowSecureLinks()),
                boolDefault(request.allowMemberVaultCreation(), current.allowMemberVaultCreation()),
                boolDefault(request.allowExport(), current.allowExport()),
                boolDefault(request.forceTwoFactor(), current.forceTwoFactor()),
                boolDefault(request.allowPasskeys(), current.allowPasskeys()),
                boolDefault(request.allowAliases(), current.allowAliases()),
                LocalDateTime.now()
        );
    }

    private void validatePasswordLengthRange(int minimumPasswordLength, int maximumPasswordLength) {
        if (minimumPasswordLength < 8 || minimumPasswordLength > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "minimumPasswordLength must be between 8 and 64");
        }
        if (maximumPasswordLength < 8 || maximumPasswordLength > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "maximumPasswordLength must be between 8 and 64");
        }
        if (maximumPasswordLength < minimumPasswordLength) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "maximumPasswordLength must be greater than or equal to minimumPasswordLength");
        }
    }

    private void savePolicySnapshot(Long orgId, Long userId, PassPolicySnapshot policy) {
        List<OrgPolicy> existing = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .in(OrgPolicy::getPolicyKey, passPolicyKeys()));
        Map<String, OrgPolicy> existingMap = existing.stream()
                .collect(Collectors.toMap(OrgPolicy::getPolicyKey, item -> item, (left, right) -> right));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH), orgId, userId, PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH, String.valueOf(policy.minimumPasswordLength()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH), orgId, userId, PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH, String.valueOf(policy.maximumPasswordLength()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_REQUIRE_UPPERCASE), orgId, userId, PassBusinessConstants.POLICY_REQUIRE_UPPERCASE, String.valueOf(policy.requireUppercase()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_REQUIRE_DIGITS), orgId, userId, PassBusinessConstants.POLICY_REQUIRE_DIGITS, String.valueOf(policy.requireDigits()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_REQUIRE_SYMBOLS), orgId, userId, PassBusinessConstants.POLICY_REQUIRE_SYMBOLS, String.valueOf(policy.requireSymbols()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS), orgId, userId, PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS, String.valueOf(policy.allowMemorablePasswords()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_EXTERNAL_SHARING), orgId, userId, PassBusinessConstants.POLICY_ALLOW_EXTERNAL_SHARING, String.valueOf(policy.allowExternalSharing()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_ITEM_SHARING), orgId, userId, PassBusinessConstants.POLICY_ALLOW_ITEM_SHARING, String.valueOf(policy.allowItemSharing()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_SECURE_LINKS), orgId, userId, PassBusinessConstants.POLICY_ALLOW_SECURE_LINKS, String.valueOf(policy.allowSecureLinks()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_MEMBER_VAULT_CREATION), orgId, userId, PassBusinessConstants.POLICY_ALLOW_MEMBER_VAULT_CREATION, String.valueOf(policy.allowMemberVaultCreation()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_EXPORT), orgId, userId, PassBusinessConstants.POLICY_ALLOW_EXPORT, String.valueOf(policy.allowExport()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_FORCE_TWO_FACTOR), orgId, userId, PassBusinessConstants.POLICY_FORCE_TWO_FACTOR, String.valueOf(policy.forceTwoFactor()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_PASSKEYS), orgId, userId, PassBusinessConstants.POLICY_ALLOW_PASSKEYS, String.valueOf(policy.allowPasskeys()));
        upsertPolicy(existingMap.get(PassBusinessConstants.POLICY_ALLOW_ALIASES), orgId, userId, PassBusinessConstants.POLICY_ALLOW_ALIASES, String.valueOf(policy.allowAliases()));
    }

    private void upsertPolicy(OrgPolicy existing, Long orgId, Long userId, String key, String value) {
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            OrgPolicy created = new OrgPolicy();
            created.setOrgId(orgId);
            created.setPolicyKey(key);
            created.setPolicyValue(value);
            created.setUpdatedBy(userId);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            created.setDeleted(0);
            orgPolicyMapper.insert(created);
            return;
        }
        existing.setPolicyValue(value);
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(now);
        existing.setDeleted(0);
        orgPolicyMapper.updateById(existing);
    }

    private List<String> passPolicyKeys() {
        return List.of(
                PassBusinessConstants.POLICY_MINIMUM_PASSWORD_LENGTH,
                PassBusinessConstants.POLICY_MAXIMUM_PASSWORD_LENGTH,
                PassBusinessConstants.POLICY_REQUIRE_UPPERCASE,
                PassBusinessConstants.POLICY_REQUIRE_DIGITS,
                PassBusinessConstants.POLICY_REQUIRE_SYMBOLS,
                PassBusinessConstants.POLICY_ALLOW_MEMORABLE_PASSWORDS,
                PassBusinessConstants.POLICY_ALLOW_EXTERNAL_SHARING,
                PassBusinessConstants.POLICY_ALLOW_ITEM_SHARING,
                PassBusinessConstants.POLICY_ALLOW_SECURE_LINKS,
                PassBusinessConstants.POLICY_ALLOW_MEMBER_VAULT_CREATION,
                PassBusinessConstants.POLICY_ALLOW_EXPORT,
                PassBusinessConstants.POLICY_FORCE_TWO_FACTOR,
                PassBusinessConstants.POLICY_ALLOW_PASSKEYS,
                PassBusinessConstants.POLICY_ALLOW_ALIASES
        );
    }

    private int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private boolean boolDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private PassBusinessPolicyVo toPolicyVo(Long orgId, PassPolicySnapshot policy) {
        return new PassBusinessPolicyVo(
                String.valueOf(orgId),
                policy.minimumPasswordLength(),
                policy.maximumPasswordLength(),
                policy.requireUppercase(),
                policy.requireDigits(),
                policy.requireSymbols(),
                policy.allowMemorablePasswords(),
                policy.allowExternalSharing(),
                policy.allowItemSharing(),
                policy.allowSecureLinks(),
                policy.allowMemberVaultCreation(),
                policy.allowExport(),
                policy.forceTwoFactor(),
                policy.allowPasskeys(),
                policy.allowAliases(),
                policy.updatedAt()
        );
    }

    private PassSharedVaultSummaryVo toVaultSummaryVo(
            PassSharedVault vault,
            String accessRole,
            Map<Long, Integer> memberCounts,
            Map<Long, Integer> itemCounts,
            Map<Long, String> creatorEmails
    ) {
        return new PassSharedVaultSummaryVo(
                String.valueOf(vault.getId()),
                String.valueOf(vault.getOrgId()),
                vault.getName(),
                vault.getDescription(),
                accessRole,
                memberCounts.getOrDefault(vault.getId(), 0),
                itemCounts.getOrDefault(vault.getId(), 0),
                creatorEmails.get(vault.getCreatedBy()),
                vault.getUpdatedAt()
        );
    }

    private PassSharedVaultMemberVo toVaultMemberVo(PassSharedVaultMember member) {
        return new PassSharedVaultMemberVo(
                String.valueOf(member.getId()),
                String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                member.getUpdatedAt()
        );
    }

    private PassItemSummaryVo toSummaryVo(PassVaultItem item, Map<Long, Integer> secureLinkCounts) {
        return new PassItemSummaryVo(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getFavorite() != null && item.getFavorite() == 1,
                item.getUpdatedAt(),
                item.getScopeType(),
                item.getItemType(),
                nullableId(item.getSharedVaultId()),
                secureLinkCounts.getOrDefault(item.getId(), 0)
        );
    }

    private PassItemDetailVo toDetailVo(PassVaultItem item, String ownerEmail, int secureLinkCount) {
        return new PassItemDetailVo(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getSecretCiphertext(),
                item.getNote(),
                item.getFavorite() != null && item.getFavorite() == 1,
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getScopeType(),
                item.getItemType(),
                nullableId(item.getOrgId()),
                nullableId(item.getSharedVaultId()),
                ownerEmail,
                secureLinkCount,
                passItemTwoFactorSupport.toVo(item)
        );
    }

    private PassSecureLinkVo toSecureLinkVo(PassSecureLink link, String publicBaseUrl) {
        return new PassSecureLinkVo(
                String.valueOf(link.getId()),
                String.valueOf(link.getItemId()),
                String.valueOf(link.getSharedVaultId()),
                link.getToken(),
                buildPublicUrl(publicBaseUrl, link.getToken()),
                link.getMaxViews(),
                link.getCurrentViews(),
                link.getExpiresAt(),
                link.getRevokedAt(),
                link.getCreatedAt(),
                isSecureLinkActive(link)
        );
    }

    private PassSecureLinkDashboardVo toSecureLinkDashboardVo(
            PassSecureLink link,
            String publicBaseUrl,
            PassVaultItem item,
            PassSharedVault vault
    ) {
        return new PassSecureLinkDashboardVo(
                String.valueOf(link.getId()),
                String.valueOf(link.getItemId()),
                String.valueOf(link.getSharedVaultId()),
                vault == null ? null : vault.getName(),
                item == null ? null : item.getTitle(),
                item == null ? null : item.getWebsite(),
                item == null ? null : item.getUsername(),
                buildPublicUrl(publicBaseUrl, link.getToken()),
                link.getMaxViews(),
                link.getCurrentViews(),
                link.getExpiresAt(),
                link.getRevokedAt(),
                link.getCreatedAt(),
                isSecureLinkActive(link),
                secureLinkStatus(link)
        );
    }

    private String buildPublicUrl(String publicBaseUrl, String token) {
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/share/pass/" + token;
    }

    private boolean isSecureLinkActive(PassSecureLink link) {
        if (link.getRevokedAt() != null) {
            return false;
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        return link.getCurrentViews() < link.getMaxViews();
    }

    private String secureLinkStatus(PassSecureLink link) {
        if (link.getRevokedAt() != null) {
            return "REVOKED";
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        if (link.getCurrentViews() >= link.getMaxViews()) {
            return "SPENT";
        }
        return "ACTIVE";
    }

    private void validateSecureLinkActive(PassSecureLink link) {
        if (link.getRevokedAt() != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link has been revoked");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link has expired");
        }
        if (link.getCurrentViews() >= link.getMaxViews()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass secure link has reached the maximum views");
        }
    }

    private void revokeSecureLinksForItem(Long itemId) {
        List<PassSecureLink> links = passSecureLinkMapper.selectList(new LambdaQueryWrapper<PassSecureLink>()
                .eq(PassSecureLink::getItemId, itemId)
                .isNull(PassSecureLink::getRevokedAt));
        if (links.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (PassSecureLink link : links) {
            link.setRevokedAt(now);
            link.setUpdatedAt(now);
            passSecureLinkMapper.updateById(link);
        }
    }

    private LocalDateTime validateExpiresAt(LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt == null) {
            return now.plusDays(DEFAULT_SECURE_LINK_EXPIRES_DAYS);
        }
        if (!expiresAt.isAfter(now)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "expiresAt must be in the future");
        }
        if (expiresAt.isAfter(now.plusDays(MAX_SECURE_LINK_EXPIRES_DAYS))) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "expiresAt must be within 30 days");
        }
        return expiresAt;
    }

    private Map<Long, PassVaultItem> loadPassItemMap(Set<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                        .in(PassVaultItem::getId, itemIds))
                .stream()
                .collect(Collectors.toMap(PassVaultItem::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private String generateUniqueSecureToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String token = UUID.randomUUID().toString().replace("-", "") + Integer.toHexString(RANDOM.nextInt(16));
            Long count = passSecureLinkMapper.selectCount(new LambdaQueryWrapper<PassSecureLink>()
                    .eq(PassSecureLink::getToken, token));
            if (count == null || count == 0) {
                return token;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to generate secure token");
    }

    private String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item title is required");
        }
        return title.trim();
    }

    private String normalizeWebsite(String website) {
        return StringUtils.hasText(website) ? website.trim() : null;
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : null;
    }

    private String normalizeNote(String note) {
        return note == null ? "" : note;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String resolveUserEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        UserAccount user = userAccountMapper.selectById(userId);
        return user == null ? null : user.getEmail();
    }

    private Map<Long, String> loadUserEmailMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getEmail));
    }

    private String nullableId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private record VaultAccessContext(OrgMember orgMember, PassSharedVault vault, String accessRole) {
    }

    private record PassPolicySnapshot(
            int minimumPasswordLength,
            int maximumPasswordLength,
            boolean requireUppercase,
            boolean requireDigits,
            boolean requireSymbols,
            boolean allowMemorablePasswords,
            boolean allowExternalSharing,
            boolean allowItemSharing,
            boolean allowSecureLinks,
            boolean allowMemberVaultCreation,
            boolean allowExport,
            boolean forceTwoFactor,
            boolean allowPasskeys,
            boolean allowAliases,
            LocalDateTime updatedAt
    ) {
    }
}
