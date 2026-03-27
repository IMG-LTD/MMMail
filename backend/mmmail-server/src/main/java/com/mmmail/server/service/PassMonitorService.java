package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.PassSharedVaultMapper;
import com.mmmail.server.mapper.PassSharedVaultMemberMapper;
import com.mmmail.server.mapper.PassVaultItemMapper;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.PassSharedVault;
import com.mmmail.server.model.entity.PassSharedVaultMember;
import com.mmmail.server.model.entity.PassVaultItem;
import com.mmmail.server.model.vo.PassMonitorItemVo;
import com.mmmail.server.model.vo.PassMonitorOverviewVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PassMonitorService {

    private static final int EXCLUDED_FLAG = 1;
    private static final int INCLUDED_FLAG = 0;

    private final PassVaultItemMapper passVaultItemMapper;
    private final PassSharedVaultMapper passSharedVaultMapper;
    private final PassSharedVaultMemberMapper passSharedVaultMemberMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final PassPasswordHealthEvaluator passwordHealthEvaluator;
    private final PassItemTwoFactorSupport passItemTwoFactorSupport;
    private final AuditService auditService;

    public PassMonitorService(
            PassVaultItemMapper passVaultItemMapper,
            PassSharedVaultMapper passSharedVaultMapper,
            PassSharedVaultMemberMapper passSharedVaultMemberMapper,
            OrgMemberMapper orgMemberMapper,
            PassPasswordHealthEvaluator passwordHealthEvaluator,
            PassItemTwoFactorSupport passItemTwoFactorSupport,
            AuditService auditService
    ) {
        this.passVaultItemMapper = passVaultItemMapper;
        this.passSharedVaultMapper = passSharedVaultMapper;
        this.passSharedVaultMemberMapper = passSharedVaultMemberMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.passwordHealthEvaluator = passwordHealthEvaluator;
        this.passItemTwoFactorSupport = passItemTwoFactorSupport;
        this.auditService = auditService;
    }

    public PassMonitorOverviewVo getPersonalMonitor(Long userId, String ipAddress) {
        List<PassVaultItem> items = passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL)
                .orderByDesc(PassVaultItem::getUpdatedAt));
        PassMonitorOverviewVo overview = buildOverview(
                PassBusinessConstants.SCOPE_PERSONAL,
                null,
                null,
                items,
                Map.of(),
                Set.of(),
                true
        );
        auditService.record(userId, "PASS_MONITOR_VIEW", "scope=PERSONAL,total=" + overview.totalItemCount(), ipAddress);
        return overview;
    }

    public PassMonitorOverviewVo getSharedMonitor(Long userId, Long orgId, String ipAddress) {
        SharedMonitorScope scope = loadSharedMonitorScope(userId, orgId);
        PassMonitorOverviewVo overview = buildOverview(
                PassBusinessConstants.SCOPE_SHARED,
                String.valueOf(orgId),
                scope.currentRole(),
                scope.items(),
                scope.vaultNames(),
                scope.manageableVaultIds(),
                false
        );
        auditService.record(
                userId,
                "PASS_MONITOR_VIEW",
                "scope=SHARED,orgId=" + orgId + ",total=" + overview.totalItemCount(),
                ipAddress,
                orgId
        );
        return overview;
    }

    @Transactional
    public void setPersonalMonitorExcluded(Long userId, Long itemId, boolean excluded, String ipAddress) {
        PassVaultItem item = loadPersonalItem(userId, itemId);
        ensureMonitorableItem(item);
        updateMonitorExcluded(item, excluded);
        auditService.record(userId, excluded ? "PASS_MONITOR_EXCLUDE" : "PASS_MONITOR_INCLUDE", "scope=PERSONAL,itemId=" + itemId, ipAddress);
    }

    @Transactional
    public void setSharedMonitorExcluded(Long userId, Long orgId, Long itemId, boolean excluded, String ipAddress) {
        PassVaultItem item = loadSharedItemForUpdate(userId, orgId, itemId);
        ensureMonitorableItem(item);
        updateMonitorExcluded(item, excluded);
        auditService.record(
                userId,
                excluded ? "PASS_MONITOR_EXCLUDE" : "PASS_MONITOR_INCLUDE",
                "scope=SHARED,orgId=" + orgId + ",itemId=" + itemId,
                ipAddress,
                orgId
        );
    }

    private PassMonitorOverviewVo buildOverview(
            String scopeType,
            String orgId,
            String currentRole,
            List<PassVaultItem> items,
            Map<Long, String> vaultNames,
            Set<Long> manageableVaultIds,
            boolean personalScope
    ) {
        List<PassVaultItem> monitorableItems = items.stream()
                .filter(passwordHealthEvaluator::supportsMonitoring)
                .toList();
        List<PassVaultItem> trackedItems = monitorableItems.stream()
                .filter(item -> !isExcluded(item))
                .toList();
        Map<String, Integer> reusedGroupSizes = buildReusedGroupSizes(trackedItems);
        return createOverview(scopeType, orgId, currentRole, monitorableItems, trackedItems, vaultNames, manageableVaultIds, personalScope, reusedGroupSizes);
    }

    private PassMonitorOverviewVo createOverview(
            String scopeType,
            String orgId,
            String currentRole,
            List<PassVaultItem> monitorableItems,
            List<PassVaultItem> trackedItems,
            Map<Long, String> vaultNames,
            Set<Long> manageableVaultIds,
            boolean personalScope,
            Map<String, Integer> reusedGroupSizes
    ) {
        List<PassMonitorItemVo> weakPasswords = new ArrayList<>();
        List<PassMonitorItemVo> reusedPasswords = new ArrayList<>();
        List<PassMonitorItemVo> inactiveTwoFactorItems = new ArrayList<>();
        List<PassMonitorItemVo> excludedItems = new ArrayList<>();
        for (PassVaultItem item : monitorableItems) {
            PassMonitorItemVo monitorItem = toMonitorItem(item, scopeType, orgId, vaultNames, manageableVaultIds, personalScope, reusedGroupSizes);
            if (monitorItem.excluded()) {
                excludedItems.add(monitorItem);
                continue;
            }
            if (monitorItem.weakPassword()) {
                weakPasswords.add(monitorItem);
            }
            if (monitorItem.reusedPassword()) {
                reusedPasswords.add(monitorItem);
            }
            if (monitorItem.inactiveTwoFactor()) {
                inactiveTwoFactorItems.add(monitorItem);
            }
        }
        return new PassMonitorOverviewVo(
                scopeType,
                orgId,
                currentRole,
                monitorableItems.size(),
                trackedItems.size(),
                weakPasswords.size(),
                reusedPasswords.size(),
                inactiveTwoFactorItems.size(),
                excludedItems.size(),
                LocalDateTime.now(),
                weakPasswords,
                reusedPasswords,
                inactiveTwoFactorItems,
                excludedItems
        );
    }

    private PassMonitorItemVo toMonitorItem(
            PassVaultItem item,
            String scopeType,
            String orgId,
            Map<Long, String> vaultNames,
            Set<Long> manageableVaultIds,
            boolean personalScope,
            Map<String, Integer> reusedGroupSizes
    ) {
        boolean excluded = isExcluded(item);
        int reusedGroupSize = excluded ? 0 : reusedGroupSizes.getOrDefault(normalizeSecret(item), 0);
        boolean canManageTwoFactor = personalScope || manageableVaultIds.contains(item.getSharedVaultId());
        return new PassMonitorItemVo(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getWebsite(),
                item.getUsername(),
                item.getItemType(),
                scopeType,
                orgId,
                nullableId(item.getSharedVaultId()),
                resolveVaultName(vaultNames, item.getSharedVaultId()),
                excluded,
                !excluded && passwordHealthEvaluator.isWeakSecret(item.getSecretCiphertext()),
                !excluded && reusedGroupSize > 1,
                !excluded && passItemTwoFactorSupport.supports(item) && !passItemTwoFactorSupport.hasTwoFactor(item),
                reusedGroupSize,
                canManageTwoFactor,
                canManageTwoFactor,
                passItemTwoFactorSupport.toVo(item),
                item.getUpdatedAt()
        );
    }

    private Map<String, Integer> buildReusedGroupSizes(List<PassVaultItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PassVaultItem item : items) {
            counts.merge(normalizeSecret(item), 1, Integer::sum);
        }
        return counts;
    }

    private SharedMonitorScope loadSharedMonitorScope(Long userId, Long orgId) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        Set<Long> accessibleVaultIds = loadVaultIds(userId, orgId, orgMember.getRole(), false);
        Set<Long> manageableVaultIds = loadVaultIds(userId, orgId, orgMember.getRole(), true);
        if (accessibleVaultIds.isEmpty()) {
            return new SharedMonitorScope(orgMember.getRole(), List.of(), Map.of(), manageableVaultIds);
        }
        List<PassVaultItem> items = passVaultItemMapper.selectList(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED)
                .in(PassVaultItem::getSharedVaultId, accessibleVaultIds)
                .orderByDesc(PassVaultItem::getUpdatedAt));
        return new SharedMonitorScope(orgMember.getRole(), items, loadVaultNames(accessibleVaultIds), manageableVaultIds);
    }

    private Set<Long> loadVaultIds(Long userId, Long orgId, String orgRole, boolean managersOnly) {
        if (isOrgManager(orgRole)) {
            return passSharedVaultMapper.selectList(new LambdaQueryWrapper<PassSharedVault>()
                            .eq(PassSharedVault::getOrgId, orgId)
                            .orderByDesc(PassSharedVault::getUpdatedAt))
                    .stream()
                    .map(PassSharedVault::getId)
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);
        }
        LambdaQueryWrapper<PassSharedVaultMember> query = new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getOrgId, orgId)
                .eq(PassSharedVaultMember::getUserId, userId);
        if (managersOnly) {
            query.eq(PassSharedVaultMember::getRole, PassBusinessConstants.VAULT_ROLE_MANAGER);
        }
        return passSharedVaultMemberMapper.selectList(query).stream()
                .map(PassSharedVaultMember::getVaultId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private Map<Long, String> loadVaultNames(Set<Long> vaultIds) {
        if (vaultIds.isEmpty()) {
            return Map.of();
        }
        return passSharedVaultMapper.selectList(new LambdaQueryWrapper<PassSharedVault>()
                        .in(PassSharedVault::getId, vaultIds))
                .stream()
                .collect(LinkedHashMap::new, (map, vault) -> map.put(vault.getId(), vault.getName()), Map::putAll);
    }

    private PassVaultItem loadPersonalItem(Long userId, Long itemId) {
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOwnerId, userId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_PERSONAL));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass item is not found");
        }
        return item;
    }

    private PassVaultItem loadSharedItemForUpdate(Long userId, Long orgId, Long itemId) {
        OrgMember orgMember = requireActiveOrgMembership(userId, orgId);
        PassVaultItem item = passVaultItemMapper.selectOne(new LambdaQueryWrapper<PassVaultItem>()
                .eq(PassVaultItem::getId, itemId)
                .eq(PassVaultItem::getOrgId, orgId)
                .eq(PassVaultItem::getScopeType, PassBusinessConstants.SCOPE_SHARED));
        if (item == null || item.getSharedVaultId() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared pass item is not found");
        }
        if (!canManageSharedItem(userId, orgMember.getRole(), item.getSharedVaultId())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Current role cannot change shared monitor exclusions");
        }
        return item;
    }

    private boolean canManageSharedItem(Long userId, String orgRole, Long vaultId) {
        if (isOrgManager(orgRole)) {
            return true;
        }
        Long count = passSharedVaultMemberMapper.selectCount(new LambdaQueryWrapper<PassSharedVaultMember>()
                .eq(PassSharedVaultMember::getVaultId, vaultId)
                .eq(PassSharedVaultMember::getUserId, userId)
                .eq(PassSharedVaultMember::getRole, PassBusinessConstants.VAULT_ROLE_MANAGER));
        return count != null && count > 0;
    }

    private OrgMember requireActiveOrgMembership(Long userId, Long orgId) {
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId));
        if (member == null || !PassBusinessConstants.ORG_STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this organization");
        }
        return member;
    }

    private void ensureMonitorableItem(PassVaultItem item) {
        if (!passwordHealthEvaluator.supportsMonitoring(item)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Pass monitor only supports password items");
        }
    }

    private void updateMonitorExcluded(PassVaultItem item, boolean excluded) {
        item.setMonitorExcluded(excluded ? EXCLUDED_FLAG : INCLUDED_FLAG);
        item.setUpdatedAt(LocalDateTime.now());
        passVaultItemMapper.updateById(item);
    }

    private boolean isExcluded(PassVaultItem item) {
        return item.getMonitorExcluded() != null && item.getMonitorExcluded() == EXCLUDED_FLAG;
    }

    private String normalizeSecret(PassVaultItem item) {
        return item.getSecretCiphertext().trim();
    }

    private boolean isOrgManager(String orgRole) {
        return PassBusinessConstants.ORG_ROLE_OWNER.equals(orgRole)
                || PassBusinessConstants.ORG_ROLE_ADMIN.equals(orgRole);
    }

    private String nullableId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveVaultName(Map<Long, String> vaultNames, Long vaultId) {
        if (vaultId == null) {
            return null;
        }
        return vaultNames.get(vaultId);
    }

    private record SharedMonitorScope(
            String currentRole,
            List<PassVaultItem> items,
            Map<Long, String> vaultNames,
            Set<Long> manageableVaultIds
    ) {
    }
}
