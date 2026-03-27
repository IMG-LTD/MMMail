package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.OrgProductAccessMapper;
import com.mmmail.server.model.dto.OrgProductAccessChangeRequest;
import com.mmmail.server.model.dto.UpdateOrgMemberProductAccessRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgProductAccess;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.vo.OrgAccessScopeVo;
import com.mmmail.server.model.vo.OrgMemberProductAccessVo;
import com.mmmail.server.model.vo.OrgProductAccessItemVo;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrgProductAccessService {

    public static final String ACCESS_ENABLED = "ENABLED";
    public static final String ACCESS_DISABLED = "DISABLED";
    private static final int NOT_DELETED = 0;
    private static final Set<String> PRODUCT_KEYS = Set.of(
            "MAIL", "CALENDAR", "DRIVE", "DOCS", "SHEETS", "PASS", "SIMPLELOGIN", "STANDARD_NOTES",
            "VPN", "WALLET", "AUTHENTICATOR", "MEET", "LUMO"
    );

    private final OrgAccessService orgAccessService;
    private final OrgProductAccessMapper orgProductAccessMapper;
    private final AuditService auditService;

    public OrgProductAccessService(
            OrgAccessService orgAccessService,
            OrgProductAccessMapper orgProductAccessMapper,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgProductAccessMapper = orgProductAccessMapper;
        this.auditService = auditService;
    }

    public List<OrgMemberProductAccessVo> listProductAccess(Long userId, Long orgId, String ipAddress) {
        OrgMember actor = orgAccessService.requireActiveMember(userId, orgId);
        List<OrgMember> members = orgAccessService.canManage(actor.getRole())
                ? orgAccessService.listActiveMembers(orgId)
                : List.of(actor);
        Map<Long, Map<String, String>> explicitStates = loadExplicitStates(orgId, members.stream().map(OrgMember::getId).toList());
        auditService.record(userId, "ORG_PRODUCT_ACCESS_LIST", "orgId=" + orgId + ",count=" + members.size(), ipAddress, orgId);
        return members.stream().map(member -> toMemberVo(actor.getUserId(), member, explicitStates.get(member.getId()))).toList();
    }

    public List<OrgAccessScopeVo> listCurrentUserAccessScopes(Long userId, String ipAddress) {
        List<OrgMember> memberships = orgAccessService.listActiveMemberships(userId);
        if (memberships.isEmpty()) {
            auditService.record(userId, "ORG_ACCESS_CONTEXT_LIST", "count=0", ipAddress);
            return List.of();
        }
        List<OrgAccessScopeVo> result = memberships.stream()
                .map(this::toAccessScopeVo)
                .toList();
        auditService.record(userId, "ORG_ACCESS_CONTEXT_LIST", "count=" + result.size(), ipAddress);
        return result;
    }

    public int countEnabledProducts(Long orgId, OrgMember member) {
        return (int) resolveStates(member, loadExplicitStates(orgId, List.of(member.getId())).get(member.getId()))
                .values()
                .stream()
                .filter(ACCESS_ENABLED::equals)
                .count();
    }

    public void assertCurrentUserProductEnabled(Long userId, Long orgId, String productKey) {
        OrgMember member = orgAccessService.requireActiveMember(userId, orgId);
        Map<String, String> states = resolveStates(member, loadExplicitStates(orgId, List.of(member.getId())).get(member.getId()));
        if (ACCESS_DISABLED.equals(states.get(productKey))) {
            throw new BizException(ErrorCode.ORG_PRODUCT_ACCESS_DENIED,
                    "Product " + productKey + " is disabled in active organization scope");
        }
    }

    public Set<String> listEnabledProductKeys(Long userId, Long orgId) {
        OrgMember member = orgAccessService.requireActiveMember(userId, orgId);
        Map<String, String> states = resolveStates(member, loadExplicitStates(orgId, List.of(member.getId())).get(member.getId()));
        Set<String> enabledProductKeys = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : states.entrySet()) {
            if (ACCESS_ENABLED.equals(entry.getValue())) {
                enabledProductKeys.add(entry.getKey());
            }
        }
        return enabledProductKeys;
    }

    @Transactional
    public OrgMemberProductAccessVo updateMemberProductAccess(
            Long userId,
            Long orgId,
            Long memberId,
            UpdateOrgMemberProductAccessRequest request,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgMember target = orgAccessService.loadActiveMemberById(orgId, memberId);
        for (OrgProductAccessChangeRequest item : request.products()) {
            applyChange(userId, orgId, memberId, item);
        }
        auditService.record(userId, "ORG_PRODUCT_ACCESS_UPDATE", "orgId=" + orgId + ",memberId=" + memberId + ",changes=" + request.products().size(), ipAddress, orgId);
        return toMemberVo(userId, target, loadExplicitStates(orgId, List.of(memberId)).get(memberId));
    }

    private void applyChange(Long userId, Long orgId, Long memberId, OrgProductAccessChangeRequest item) {
        OrgProductAccess existing = orgProductAccessMapper.selectIncludingDeleted(orgId, memberId, item.productKey());
        LocalDateTime now = LocalDateTime.now();
        if (shouldEnable(item.accessState())) {
            removeActiveOverride(existing);
            return;
        }
        if (existing == null) {
            insertDisabledAccess(userId, orgId, memberId, item.productKey(), now);
            return;
        }
        if (isSoftDeleted(existing)) {
            restoreDisabledAccess(existing.getId(), userId, now);
            return;
        }
        updateDisabledAccess(existing, userId, now);
    }

    private boolean shouldEnable(String accessState) {
        return ACCESS_ENABLED.equals(accessState);
    }

    private void removeActiveOverride(OrgProductAccess existing) {
        if (existing == null || isSoftDeleted(existing)) {
            return;
        }
        orgProductAccessMapper.deleteById(existing.getId());
    }

    private boolean isSoftDeleted(OrgProductAccess access) {
        return access.getDeleted() != null && access.getDeleted() != NOT_DELETED;
    }

    private void restoreDisabledAccess(Long accessId, Long userId, LocalDateTime now) {
        orgProductAccessMapper.restoreDisabledAccess(accessId, ACCESS_DISABLED, userId, now);
    }

    private void updateDisabledAccess(OrgProductAccess existing, Long userId, LocalDateTime now) {
        existing.setAccessState(ACCESS_DISABLED);
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(now);
        orgProductAccessMapper.updateById(existing);
    }

    private void insertDisabledAccess(Long userId, Long orgId, Long memberId, String productKey, LocalDateTime now) {
        OrgProductAccess access = new OrgProductAccess();
        access.setOrgId(orgId);
        access.setMemberId(memberId);
        access.setProductKey(productKey);
        access.setAccessState(ACCESS_DISABLED);
        access.setUpdatedBy(userId);
        access.setCreatedAt(now);
        access.setUpdatedAt(now);
        access.setDeleted(NOT_DELETED);
        orgProductAccessMapper.insert(access);
    }

    private Map<Long, Map<String, String>> loadExplicitStates(Long orgId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        List<OrgProductAccess> rows = orgProductAccessMapper.selectList(new LambdaQueryWrapper<OrgProductAccess>()
                .eq(OrgProductAccess::getOrgId, orgId)
                .in(OrgProductAccess::getMemberId, memberIds));
        Map<Long, Map<String, String>> result = new LinkedHashMap<>();
        for (OrgProductAccess row : rows) {
            result.computeIfAbsent(row.getMemberId(), ignored -> new LinkedHashMap<>())
                    .put(row.getProductKey(), row.getAccessState());
        }
        return result;
    }

    private OrgMemberProductAccessVo toMemberVo(Long currentUserId, OrgMember member, Map<String, String> explicitStates) {
        Map<String, String> states = resolveStates(member, explicitStates);
        List<OrgProductAccessItemVo> products = toProductItems(states);
        int enabledCount = (int) products.stream().filter(item -> ACCESS_ENABLED.equals(item.accessState())).count();
        return new OrgMemberProductAccessVo(
                String.valueOf(member.getId()),
                member.getUserId() == null ? null : String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                member.getUserId() != null && member.getUserId().equals(currentUserId),
                enabledCount,
                products
        );
    }

    private OrgAccessScopeVo toAccessScopeVo(OrgMember member) {
        OrgWorkspace org = orgAccessService.loadOrg(member.getOrgId());
        Map<String, String> states = resolveStates(member, loadExplicitStates(org.getId(), List.of(member.getId())).get(member.getId()));
        List<OrgProductAccessItemVo> products = toProductItems(states);
        int enabledCount = (int) products.stream().filter(item -> ACCESS_ENABLED.equals(item.accessState())).count();
        return new OrgAccessScopeVo(
                String.valueOf(org.getId()),
                org.getName(),
                org.getSlug(),
                member.getRole(),
                enabledCount,
                products
        );
    }

    private List<OrgProductAccessItemVo> toProductItems(Map<String, String> states) {
        return PRODUCT_KEYS.stream()
                .sorted()
                .map(productKey -> new OrgProductAccessItemVo(productKey, states.get(productKey)))
                .toList();
    }

    private Map<String, String> resolveStates(OrgMember member, Map<String, String> explicitStates) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String productKey : PRODUCT_KEYS) {
            result.put(productKey, ACCESS_ENABLED);
        }
        if (explicitStates == null || explicitStates.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : explicitStates.entrySet()) {
            if (PRODUCT_KEYS.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
