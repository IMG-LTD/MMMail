package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthenticatorEntryMapper;
import com.mmmail.server.mapper.OrgPolicyMapper;
import com.mmmail.server.model.dto.UpdateOrgPolicyRequest;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.vo.OrgPolicyVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrgPolicyService {

    public static final String TWO_FACTOR_ENFORCEMENT_OFF = "OFF";
    public static final String TWO_FACTOR_ENFORCEMENT_ADMINS = "ADMINS";
    public static final String TWO_FACTOR_ENFORCEMENT_ALL = "ALL";

    private static final String POLICY_ALLOWED_EMAIL_DOMAINS = "allowedEmailDomains";
    private static final String POLICY_MEMBER_LIMIT = "memberLimit";
    private static final String POLICY_ADMIN_CAN_INVITE_ADMIN = "adminCanInviteAdmin";
    private static final String POLICY_ADMIN_CAN_REMOVE_ADMIN = "adminCanRemoveAdmin";
    private static final String POLICY_ADMIN_CAN_REVIEW_GOVERNANCE = "adminCanReviewGovernance";
    private static final String POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE = "adminCanExecuteGovernance";
    private static final String POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE = "requireDualReviewGovernance";
    private static final String POLICY_GOVERNANCE_REVIEW_SLA_HOURS = "governanceReviewSlaHours";
    private static final String POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL = "twoFactorEnforcementLevel";
    private static final String POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS = "twoFactorGracePeriodDays";

    private static final int DEFAULT_MEMBER_LIMIT = 200;
    private static final int DEFAULT_GOVERNANCE_REVIEW_SLA_HOURS = 24;
    private static final int DEFAULT_TWO_FACTOR_GRACE_PERIOD_DAYS = 0;
    private static final int MAX_TWO_FACTOR_GRACE_PERIOD_DAYS = 90;

    private static final String EVENT_POLICY_UPDATE = "ORG_POLICY_UPDATE";
    private static final String EVENT_TWO_FACTOR_BLOCK = "ORG_AUTH_2FA_ENFORCEMENT_BLOCK";
    private static final String EVENT_TWO_FACTOR_REQUIREMENT_ENABLED = "ORG_AUTH_2FA_REQUIREMENT_ENABLED";
    private static final String EVENT_TWO_FACTOR_REQUIREMENT_DISABLED = "ORG_AUTH_2FA_REQUIREMENT_DISABLED";
    private static final String EVENT_TWO_FACTOR_GRACE_PERIOD_CHANGED = "ORG_AUTH_2FA_GRACE_PERIOD_CHANGED";

    private final OrgPolicyMapper orgPolicyMapper;
    private final OrgAccessService orgAccessService;
    private final AuditService auditService;
    private final AuthenticatorEntryMapper authenticatorEntryMapper;

    public OrgPolicyService(
            OrgPolicyMapper orgPolicyMapper,
            OrgAccessService orgAccessService,
            AuditService auditService,
            AuthenticatorEntryMapper authenticatorEntryMapper
    ) {
        this.orgPolicyMapper = orgPolicyMapper;
        this.orgAccessService = orgAccessService;
        this.auditService = auditService;
        this.authenticatorEntryMapper = authenticatorEntryMapper;
    }

    public OrgPolicyVo getPolicy(Long userId, Long orgId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        OrgPolicySnapshot policy = loadPolicySnapshot(orgId);
        auditService.record(userId, "ORG_POLICY_GET", "orgId=" + orgId, ipAddress, orgId);
        return toOrgPolicyVo(orgId, policy);
    }

    @Transactional
    public OrgPolicyVo updatePolicy(Long userId, Long orgId, UpdateOrgPolicyRequest request, String ipAddress) {
        OrgMember actorMember = orgAccessService.requireActiveMember(userId, orgId);
        if (!OrgAccessService.ROLE_OWNER.equals(actorMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER can update organization policy");
        }
        OrgPolicySnapshot current = loadPolicySnapshot(orgId);
        OrgPolicySnapshot next = mergePolicySnapshot(current, request);
        savePolicySnapshot(orgId, userId, next);
        auditService.record(userId, EVENT_POLICY_UPDATE, buildUpdateAuditDetail(orgId, next), ipAddress, orgId);
        recordTwoFactorPolicyEvents(userId, orgId, current, next, ipAddress);
        return toOrgPolicyVo(orgId, next);
    }

    public OrgPolicySnapshot loadPolicySnapshot(Long orgId) {
        List<OrgPolicy> items = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .orderByDesc(OrgPolicy::getUpdatedAt));
        Map<String, PolicyValue> valueMap = new LinkedHashMap<>();
        LocalDateTime latestUpdatedAt = null;
        for (OrgPolicy item : items) {
            valueMap.putIfAbsent(item.getPolicyKey(), new PolicyValue(item.getPolicyValue(), item.getUpdatedAt()));
            latestUpdatedAt = resolveLatestUpdatedAt(latestUpdatedAt, item.getUpdatedAt());
        }
        return new OrgPolicySnapshot(
                parseAllowedDomains(readValue(valueMap, POLICY_ALLOWED_EMAIL_DOMAINS)),
                parseMemberLimit(readValue(valueMap, POLICY_MEMBER_LIMIT)),
                parseGovernanceReviewSlaHours(readValue(valueMap, POLICY_GOVERNANCE_REVIEW_SLA_HOURS)),
                parseBoolean(readValue(valueMap, POLICY_ADMIN_CAN_INVITE_ADMIN)),
                parseBoolean(readValue(valueMap, POLICY_ADMIN_CAN_REMOVE_ADMIN)),
                parseBoolean(readValue(valueMap, POLICY_ADMIN_CAN_REVIEW_GOVERNANCE)),
                parseBoolean(readValue(valueMap, POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE)),
                parseBoolean(readValue(valueMap, POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE)),
                parseTwoFactorEnforcementLevel(readValue(valueMap, POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL)),
                parseTwoFactorGracePeriodDays(readValue(valueMap, POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS)),
                readUpdatedAt(valueMap, POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL),
                readUpdatedAt(valueMap, POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS),
                latestUpdatedAt
        );
    }

    public void assertTwoFactorCompliant(Long userId, Long orgId, String productKey, String ipAddress) {
        OrgMember member = orgAccessService.requireActiveMember(userId, orgId);
        OrgPolicySnapshot policy = loadPolicySnapshot(orgId);
        TwoFactorAccessState accessState = evaluateTwoFactorAccessState(member, policy, hasAuthenticatorEntry(userId));
        if (!accessState.blockedByPolicy()) {
            return;
        }
        auditService.record(
                userId,
                EVENT_TWO_FACTOR_BLOCK,
                buildEnforcementBlockDetail(orgId, member, productKey, policy, accessState),
                ipAddress,
                orgId
        );
        throw new BizException(
                ErrorCode.ORG_TWO_FACTOR_REQUIRED,
                "Organization policy requires two-factor authentication before you can continue. Open /authenticator to recover access."
        );
    }

    public boolean hasAuthenticatorEntry(Long userId) {
        Long count = authenticatorEntryMapper.selectCount(new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getOwnerId, userId));
        return count != null && count > 0;
    }

    public TwoFactorAccessState evaluateTwoFactorAccessState(
            OrgMember member,
            OrgPolicySnapshot policy,
            boolean hasAuthenticatorEntry
    ) {
        if (!policy.requiresTwoFactor(member.getRole()) || hasAuthenticatorEntry) {
            return TwoFactorAccessState.compliant();
        }
        LocalDateTime gracePeriodEndsAt = policy.resolveGracePeriodEndsAt(member.getJoinedAt(), member.getCreatedAt());
        if (policy.twoFactorGracePeriodDays() == 0) {
            return TwoFactorAccessState.blocked(gracePeriodEndsAt);
        }
        if (gracePeriodEndsAt != null && LocalDateTime.now().isBefore(gracePeriodEndsAt)) {
            return TwoFactorAccessState.inGracePeriod(gracePeriodEndsAt);
        }
        return TwoFactorAccessState.blocked(gracePeriodEndsAt);
    }

    private LocalDateTime resolveLatestUpdatedAt(LocalDateTime current, LocalDateTime candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null || !candidate.isAfter(current)) {
            return current;
        }
        return candidate;
    }

    private OrgPolicySnapshot mergePolicySnapshot(OrgPolicySnapshot current, UpdateOrgPolicyRequest request) {
        List<String> allowedDomains = request.allowedEmailDomains() == null
                ? current.allowedEmailDomains()
                : normalizeDomainList(request.allowedEmailDomains());
        int memberLimit = resolveMemberLimit(current.memberLimit(), request.memberLimit());
        int governanceReviewSlaHours = resolveGovernanceReviewSlaHours(
                current.governanceReviewSlaHours(),
                request.governanceReviewSlaHours()
        );
        String enforcementLevel = normalizeTwoFactorEnforcementLevel(
                request.twoFactorEnforcementLevel(),
                current.twoFactorEnforcementLevel()
        );
        int gracePeriodDays = normalizeTwoFactorGracePeriodDays(
                request.twoFactorGracePeriodDays(),
                current.twoFactorGracePeriodDays()
        );
        LocalDateTime now = LocalDateTime.now();
        return new OrgPolicySnapshot(
                allowedDomains,
                memberLimit,
                governanceReviewSlaHours,
                boolDefault(request.adminCanInviteAdmin(), current.adminCanInviteAdmin()),
                boolDefault(request.adminCanRemoveAdmin(), current.adminCanRemoveAdmin()),
                boolDefault(request.adminCanReviewGovernance(), current.adminCanReviewGovernance()),
                boolDefault(request.adminCanExecuteGovernance(), current.adminCanExecuteGovernance()),
                boolDefault(request.requireDualReviewGovernance(), current.requireDualReviewGovernance()),
                enforcementLevel,
                gracePeriodDays,
                resolvePolicyUpdatedAt(current.twoFactorEnforcementUpdatedAt(), current.twoFactorEnforcementLevel(), enforcementLevel, now),
                resolvePolicyUpdatedAt(current.twoFactorGracePeriodUpdatedAt(), current.twoFactorGracePeriodDays(), gracePeriodDays, now),
                now
        );
    }

    private int resolveMemberLimit(int currentValue, Integer nextValue) {
        int value = nextValue == null ? currentValue : nextValue;
        if (value < 1 || value > 5000) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "memberLimit must be between 1 and 5000");
        }
        return value;
    }

    private int resolveGovernanceReviewSlaHours(int currentValue, Integer nextValue) {
        int value = nextValue == null ? currentValue : nextValue;
        if (value < 1 || value > 168) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "governanceReviewSlaHours must be between 1 and 168");
        }
        return value;
    }

    private boolean boolDefault(Boolean candidate, boolean fallback) {
        return candidate == null ? fallback : candidate;
    }

    private int normalizeTwoFactorGracePeriodDays(Integer raw, int fallback) {
        int value = raw == null ? fallback : raw;
        if (value < 0 || value > MAX_TWO_FACTOR_GRACE_PERIOD_DAYS) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "twoFactorGracePeriodDays must be between 0 and " + MAX_TWO_FACTOR_GRACE_PERIOD_DAYS
            );
        }
        return value;
    }

    private String normalizeTwoFactorEnforcementLevel(String raw, String fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(TWO_FACTOR_ENFORCEMENT_OFF, TWO_FACTOR_ENFORCEMENT_ADMINS, TWO_FACTOR_ENFORCEMENT_ALL).contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "twoFactorEnforcementLevel must be OFF, ADMINS, or ALL");
        }
        return normalized;
    }

    private void savePolicySnapshot(Long orgId, Long userId, OrgPolicySnapshot policy) {
        List<OrgPolicy> existing = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId)
                .in(OrgPolicy::getPolicyKey, List.of(
                        POLICY_ALLOWED_EMAIL_DOMAINS,
                        POLICY_MEMBER_LIMIT,
                        POLICY_GOVERNANCE_REVIEW_SLA_HOURS,
                        POLICY_ADMIN_CAN_INVITE_ADMIN,
                        POLICY_ADMIN_CAN_REMOVE_ADMIN,
                        POLICY_ADMIN_CAN_REVIEW_GOVERNANCE,
                        POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE,
                        POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE,
                        POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL,
                        POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS
                )));
        Map<String, OrgPolicy> existingMap = existing.stream()
                .collect(java.util.stream.Collectors.toMap(OrgPolicy::getPolicyKey, item -> item, (left, right) -> right));
        upsertPolicy(existingMap.get(POLICY_ALLOWED_EMAIL_DOMAINS), orgId, userId, POLICY_ALLOWED_EMAIL_DOMAINS, String.join(",", policy.allowedEmailDomains()));
        upsertPolicy(existingMap.get(POLICY_MEMBER_LIMIT), orgId, userId, POLICY_MEMBER_LIMIT, String.valueOf(policy.memberLimit()));
        upsertPolicy(existingMap.get(POLICY_GOVERNANCE_REVIEW_SLA_HOURS), orgId, userId, POLICY_GOVERNANCE_REVIEW_SLA_HOURS, String.valueOf(policy.governanceReviewSlaHours()));
        upsertPolicy(existingMap.get(POLICY_ADMIN_CAN_INVITE_ADMIN), orgId, userId, POLICY_ADMIN_CAN_INVITE_ADMIN, String.valueOf(policy.adminCanInviteAdmin()));
        upsertPolicy(existingMap.get(POLICY_ADMIN_CAN_REMOVE_ADMIN), orgId, userId, POLICY_ADMIN_CAN_REMOVE_ADMIN, String.valueOf(policy.adminCanRemoveAdmin()));
        upsertPolicy(existingMap.get(POLICY_ADMIN_CAN_REVIEW_GOVERNANCE), orgId, userId, POLICY_ADMIN_CAN_REVIEW_GOVERNANCE, String.valueOf(policy.adminCanReviewGovernance()));
        upsertPolicy(existingMap.get(POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE), orgId, userId, POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE, String.valueOf(policy.adminCanExecuteGovernance()));
        upsertPolicy(existingMap.get(POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE), orgId, userId, POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE, String.valueOf(policy.requireDualReviewGovernance()));
        upsertPolicy(existingMap.get(POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL), orgId, userId, POLICY_TWO_FACTOR_ENFORCEMENT_LEVEL, policy.twoFactorEnforcementLevel());
        upsertPolicy(existingMap.get(POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS), orgId, userId, POLICY_TWO_FACTOR_GRACE_PERIOD_DAYS, String.valueOf(policy.twoFactorGracePeriodDays()));
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

    private OrgPolicyVo toOrgPolicyVo(Long orgId, OrgPolicySnapshot policy) {
        return new OrgPolicyVo(
                String.valueOf(orgId),
                policy.allowedEmailDomains(),
                policy.memberLimit(),
                policy.governanceReviewSlaHours(),
                policy.adminCanInviteAdmin(),
                policy.adminCanRemoveAdmin(),
                policy.adminCanReviewGovernance(),
                policy.adminCanExecuteGovernance(),
                policy.requireDualReviewGovernance(),
                policy.twoFactorEnforcementLevel(),
                policy.twoFactorGracePeriodDays(),
                policy.updatedAt()
        );
    }

    private String buildUpdateAuditDetail(Long orgId, OrgPolicySnapshot policy) {
        return "orgId=" + orgId
                + ",memberLimit=" + policy.memberLimit()
                + ",governanceReviewSlaHours=" + policy.governanceReviewSlaHours()
                + ",allowedDomains=" + String.join("|", policy.allowedEmailDomains())
                + ",adminCanInviteAdmin=" + policy.adminCanInviteAdmin()
                + ",adminCanRemoveAdmin=" + policy.adminCanRemoveAdmin()
                + ",adminCanReviewGovernance=" + policy.adminCanReviewGovernance()
                + ",adminCanExecuteGovernance=" + policy.adminCanExecuteGovernance()
                + ",requireDualReviewGovernance=" + policy.requireDualReviewGovernance()
                + ",twoFactorEnforcementLevel=" + policy.twoFactorEnforcementLevel()
                + ",twoFactorGracePeriodDays=" + policy.twoFactorGracePeriodDays();
    }

    private String buildEnforcementBlockDetail(
            Long orgId,
            OrgMember member,
            String productKey,
            OrgPolicySnapshot policy,
            TwoFactorAccessState accessState
    ) {
        return "orgId=" + orgId
                + ",memberId=" + member.getId()
                + ",role=" + member.getRole()
                + ",product=" + productKey
                + ",enforcement=" + policy.twoFactorEnforcementLevel()
                + ",gracePeriodDays=" + policy.twoFactorGracePeriodDays()
                + ",gracePeriodEndsAt=" + accessState.gracePeriodEndsAt();
    }

    private List<String> parseAllowedDomains(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return normalizeDomainList(List.of(raw.split(",")));
    }

    private List<String> normalizeDomainList(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : domains) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String domain = item.trim().toLowerCase(Locale.ROOT);
            if (domain.startsWith("@")) {
                domain = domain.substring(1);
            }
            if (!domain.matches("^[a-z0-9.-]+\\.[a-z]{2,}$")) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid email domain: " + item);
            }
            normalized.add(domain);
        }
        return List.copyOf(normalized);
    }

    private int parseMemberLimit(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_MEMBER_LIMIT;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed < 1 ? DEFAULT_MEMBER_LIMIT : parsed;
        } catch (NumberFormatException ex) {
            return DEFAULT_MEMBER_LIMIT;
        }
    }

    private int parseGovernanceReviewSlaHours(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_GOVERNANCE_REVIEW_SLA_HOURS;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed < 1 || parsed > 168 ? DEFAULT_GOVERNANCE_REVIEW_SLA_HOURS : parsed;
        } catch (NumberFormatException ex) {
            return DEFAULT_GOVERNANCE_REVIEW_SLA_HOURS;
        }
    }

    private boolean parseBoolean(String raw) {
        return StringUtils.hasText(raw) && Boolean.parseBoolean(raw.trim());
    }

    private String parseTwoFactorEnforcementLevel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return TWO_FACTOR_ENFORCEMENT_OFF;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return Set.of(TWO_FACTOR_ENFORCEMENT_OFF, TWO_FACTOR_ENFORCEMENT_ADMINS, TWO_FACTOR_ENFORCEMENT_ALL).contains(normalized)
                ? normalized
                : TWO_FACTOR_ENFORCEMENT_OFF;
    }

    private int parseTwoFactorGracePeriodDays(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_TWO_FACTOR_GRACE_PERIOD_DAYS;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 0 || parsed > MAX_TWO_FACTOR_GRACE_PERIOD_DAYS) {
                return DEFAULT_TWO_FACTOR_GRACE_PERIOD_DAYS;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return DEFAULT_TWO_FACTOR_GRACE_PERIOD_DAYS;
        }
    }

    private String readValue(Map<String, PolicyValue> valueMap, String key) {
        PolicyValue value = valueMap.get(key);
        return value == null ? null : value.value();
    }

    private LocalDateTime readUpdatedAt(Map<String, PolicyValue> valueMap, String key) {
        PolicyValue value = valueMap.get(key);
        return value == null ? null : value.updatedAt();
    }

    private LocalDateTime resolvePolicyUpdatedAt(
            LocalDateTime currentUpdatedAt,
            Object currentValue,
            Object nextValue,
            LocalDateTime fallbackNow
    ) {
        if (java.util.Objects.equals(currentValue, nextValue)) {
            return currentUpdatedAt;
        }
        return fallbackNow;
    }

    private void recordTwoFactorPolicyEvents(
            Long userId,
            Long orgId,
            OrgPolicySnapshot current,
            OrgPolicySnapshot next,
            String ipAddress
    ) {
        if (!current.twoFactorEnforcementLevel().equals(next.twoFactorEnforcementLevel())) {
            String eventType = isTwoFactorRequirementEnabled(next.twoFactorEnforcementLevel())
                    ? EVENT_TWO_FACTOR_REQUIREMENT_ENABLED
                    : EVENT_TWO_FACTOR_REQUIREMENT_DISABLED;
            auditService.record(userId, eventType, buildRequirementAuditDetail(orgId, current, next), ipAddress, orgId);
        }
        if (current.twoFactorGracePeriodDays() != next.twoFactorGracePeriodDays()) {
            auditService.record(
                    userId,
                    EVENT_TWO_FACTOR_GRACE_PERIOD_CHANGED,
                    buildGracePeriodAuditDetail(orgId, current, next),
                    ipAddress,
                    orgId
            );
        }
    }

    private boolean isTwoFactorRequirementEnabled(String enforcementLevel) {
        return !TWO_FACTOR_ENFORCEMENT_OFF.equals(enforcementLevel);
    }

    private String buildRequirementAuditDetail(Long orgId, OrgPolicySnapshot current, OrgPolicySnapshot next) {
        return "orgId=" + orgId
                + ",previousEnforcement=" + current.twoFactorEnforcementLevel()
                + ",currentEnforcement=" + next.twoFactorEnforcementLevel()
                + ",gracePeriodDays=" + next.twoFactorGracePeriodDays();
    }

    private String buildGracePeriodAuditDetail(Long orgId, OrgPolicySnapshot current, OrgPolicySnapshot next) {
        return "orgId=" + orgId
                + ",previousGracePeriodDays=" + current.twoFactorGracePeriodDays()
                + ",currentGracePeriodDays=" + next.twoFactorGracePeriodDays()
                + ",enforcement=" + next.twoFactorEnforcementLevel();
    }

    public record OrgPolicySnapshot(
            List<String> allowedEmailDomains,
            int memberLimit,
            int governanceReviewSlaHours,
            boolean adminCanInviteAdmin,
            boolean adminCanRemoveAdmin,
            boolean adminCanReviewGovernance,
            boolean adminCanExecuteGovernance,
            boolean requireDualReviewGovernance,
            String twoFactorEnforcementLevel,
            int twoFactorGracePeriodDays,
            LocalDateTime twoFactorEnforcementUpdatedAt,
            LocalDateTime twoFactorGracePeriodUpdatedAt,
            LocalDateTime updatedAt
    ) {
        public boolean requiresTwoFactor(String role) {
            if (TWO_FACTOR_ENFORCEMENT_ALL.equals(twoFactorEnforcementLevel)) {
                return OrgAccessService.ROLE_OWNER.equals(role)
                        || OrgAccessService.ROLE_ADMIN.equals(role)
                        || OrgAccessService.ROLE_MEMBER.equals(role);
            }
            if (TWO_FACTOR_ENFORCEMENT_ADMINS.equals(twoFactorEnforcementLevel)) {
                return OrgAccessService.ROLE_OWNER.equals(role) || OrgAccessService.ROLE_ADMIN.equals(role);
            }
            return false;
        }

        public LocalDateTime resolveGracePeriodEndsAt(LocalDateTime joinedAt, LocalDateTime createdAt) {
            LocalDateTime graceStart = resolveGracePeriodStart(joinedAt, createdAt);
            if (graceStart == null) {
                return null;
            }
            return graceStart.plusDays(twoFactorGracePeriodDays);
        }

        private LocalDateTime resolveGracePeriodStart(LocalDateTime joinedAt, LocalDateTime createdAt) {
            LocalDateTime memberStart = joinedAt != null ? joinedAt : createdAt;
            LocalDateTime policyStart = resolveRequirementEffectiveAt();
            if (memberStart == null) {
                return policyStart;
            }
            if (policyStart == null || memberStart.isAfter(policyStart)) {
                return memberStart;
            }
            return policyStart;
        }

        private LocalDateTime resolveRequirementEffectiveAt() {
            if (twoFactorEnforcementUpdatedAt == null) {
                return twoFactorGracePeriodUpdatedAt;
            }
            if (twoFactorGracePeriodUpdatedAt == null || !twoFactorGracePeriodUpdatedAt.isAfter(twoFactorEnforcementUpdatedAt)) {
                return twoFactorEnforcementUpdatedAt;
            }
            return twoFactorGracePeriodUpdatedAt;
        }
    }

    public record TwoFactorAccessState(
            boolean requiresSetup,
            boolean inGracePeriod,
            boolean blockedByPolicy,
            LocalDateTime gracePeriodEndsAt
    ) {
        private static TwoFactorAccessState compliant() {
            return new TwoFactorAccessState(false, false, false, null);
        }

        private static TwoFactorAccessState inGracePeriod(LocalDateTime gracePeriodEndsAt) {
            return new TwoFactorAccessState(true, true, false, gracePeriodEndsAt);
        }

        private static TwoFactorAccessState blocked(LocalDateTime gracePeriodEndsAt) {
            return new TwoFactorAccessState(true, false, true, gracePeriodEndsAt);
        }
    }

    private record PolicyValue(String value, LocalDateTime updatedAt) {
    }
}
