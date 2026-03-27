package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthenticatorEntryMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.dto.SendOrgAuthenticationSecurityReminderRequest;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.OrgAuthenticationSecurityMemberVo;
import com.mmmail.server.model.vo.OrgAuthenticationSecurityReminderResultVo;
import com.mmmail.server.model.vo.OrgAuthenticationSecurityVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrgAuthenticationSecurityService {

    private static final int DEFAULT_LIMIT = 60;
    private static final int MAX_LIMIT = 120;
    private static final int REMINDER_AUDIT_LIMIT = 200;
    private static final String EVENT_VIEW = "ORG_AUTH_SECURITY_VIEW";
    private static final String EVENT_REMINDER_SENT = "ORG_AUTH_2FA_REMINDER_SENT";

    private final OrgAccessService orgAccessService;
    private final OrgPolicyService orgPolicyService;
    private final AuthenticatorEntryMapper authenticatorEntryMapper;
    private final UserSessionMapper userSessionMapper;
    private final AuditService auditService;
    private final OrgSecurityReminderMailService orgSecurityReminderMailService;

    public OrgAuthenticationSecurityService(
            OrgAccessService orgAccessService,
            OrgPolicyService orgPolicyService,
            AuthenticatorEntryMapper authenticatorEntryMapper,
            UserSessionMapper userSessionMapper,
            AuditService auditService,
            OrgSecurityReminderMailService orgSecurityReminderMailService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgPolicyService = orgPolicyService;
        this.authenticatorEntryMapper = authenticatorEntryMapper;
        this.userSessionMapper = userSessionMapper;
        this.auditService = auditService;
        this.orgSecurityReminderMailService = orgSecurityReminderMailService;
    }

    public OrgAuthenticationSecurityVo getOverview(
            Long actorUserId,
            Long orgId,
            String memberEmail,
            Boolean onlyWithoutTwoFactor,
            Integer limit,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(actorUserId, orgId);
        List<OrgMember> members = orgAccessService.listActiveMembers(orgId);
        Map<Long, TwoFactorStats> statsMap = loadTwoFactorStats(members);
        Map<Long, Integer> sessionCountMap = loadActiveSessionCounts(members);
        Map<Long, LocalDateTime> reminderMap = loadLatestReminderMap(orgId);
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);
        List<OrgAuthenticationSecurityMemberVo> rows = buildRows(
                members,
                statsMap,
                sessionCountMap,
                reminderMap,
                policy,
                memberEmail,
                Boolean.TRUE.equals(onlyWithoutTwoFactor),
                normalizeLimit(limit)
        );
        auditService.record(actorUserId, EVENT_VIEW, buildViewAuditDetail(orgId, memberEmail, onlyWithoutTwoFactor, rows.size()), ipAddress, orgId);
        return buildOverview(orgId, members, statsMap, rows, policy);
    }

    public OrgAuthenticationSecurityReminderResultVo sendReminders(
            Long actorUserId,
            Long orgId,
            SendOrgAuthenticationSecurityReminderRequest request,
            String ipAddress
    ) {
        OrgMember actor = orgAccessService.requireManageMember(actorUserId, orgId);
        OrgWorkspace org = orgAccessService.loadOrg(orgId);
        List<Long> normalizedMemberIds = normalizeMemberIds(request.memberIds());
        Map<Long, OrgMember> memberMap = loadTargetMembers(orgId, normalizedMemberIds);
        Map<Long, TwoFactorStats> statsMap = loadTwoFactorStats(new ArrayList<>(memberMap.values()));
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);
        return dispatchReminders(actor, org, normalizedMemberIds, memberMap, statsMap, policy, ipAddress);
    }

    private OrgAuthenticationSecurityVo buildOverview(
            Long orgId,
            List<OrgMember> members,
            Map<Long, TwoFactorStats> statsMap,
            List<OrgAuthenticationSecurityMemberVo> rows,
            OrgPolicyService.OrgPolicySnapshot policy
    ) {
        int protectedMembers = 0;
        int protectedManagers = 0;
        int blockedMembers = 0;
        for (OrgMember member : members) {
            boolean protectedMember = isTwoFactorEnabled(statsMap, member.getUserId());
            OrgPolicyService.TwoFactorAccessState accessState = orgPolicyService.evaluateTwoFactorAccessState(member, policy, protectedMember);
            if (protectedMember) {
                protectedMembers++;
            }
            if (isManager(member) && protectedMember) {
                protectedManagers++;
            }
            if (accessState.blockedByPolicy()) {
                blockedMembers++;
            }
        }
        int totalMembers = members.size();
        int unprotectedMembers = totalMembers - protectedMembers;
        int totalManagers = (int) members.stream().filter(this::isManager).count();
        return new OrgAuthenticationSecurityVo(
                String.valueOf(orgId),
                policy.twoFactorEnforcementLevel(),
                policy.twoFactorGracePeriodDays(),
                totalMembers,
                protectedMembers,
                unprotectedMembers,
                protectedManagers,
                totalManagers - protectedManagers,
                blockedMembers,
                rows,
                LocalDateTime.now()
        );
    }

    private List<OrgAuthenticationSecurityMemberVo> buildRows(
            List<OrgMember> members,
            Map<Long, TwoFactorStats> statsMap,
            Map<Long, Integer> sessionCountMap,
            Map<Long, LocalDateTime> reminderMap,
            OrgPolicyService.OrgPolicySnapshot policy,
            String memberEmail,
            boolean onlyWithoutTwoFactor,
            int limit
    ) {
        List<OrgAuthenticationSecurityMemberVo> rows = new ArrayList<>();
        for (OrgMember member : members) {
            OrgAuthenticationSecurityMemberVo row = toMemberVo(member, statsMap, sessionCountMap, reminderMap, policy);
            if (!matchesFilter(row, memberEmail, onlyWithoutTwoFactor)) {
                continue;
            }
            rows.add(row);
            if (rows.size() >= limit) {
                break;
            }
        }
        return rows;
    }

    private OrgAuthenticationSecurityReminderResultVo dispatchReminders(
            OrgMember actor,
            OrgWorkspace org,
            List<Long> memberIds,
            Map<Long, OrgMember> memberMap,
            Map<Long, TwoFactorStats> statsMap,
            OrgPolicyService.OrgPolicySnapshot policy,
            String ipAddress
    ) {
        List<String> deliveredIds = new ArrayList<>();
        int skippedProtected = 0;
        int skippedMissing = 0;
        for (Long memberId : memberIds) {
            OrgMember target = memberMap.get(memberId);
            if (target == null || target.getUserId() == null) {
                skippedMissing++;
                continue;
            }
            if (isTwoFactorEnabled(statsMap, target.getUserId())) {
                skippedProtected++;
                continue;
            }
            orgSecurityReminderMailService.sendTwoFactorReminder(
                    org,
                    actor,
                    target,
                    policy.twoFactorEnforcementLevel(),
                    policy.twoFactorGracePeriodDays()
            );
            auditService.record(actor.getUserId(), EVENT_REMINDER_SENT, buildReminderAuditDetail(org.getId(), actor, target, policy), ipAddress, org.getId());
            deliveredIds.add(String.valueOf(target.getId()));
        }
        return new OrgAuthenticationSecurityReminderResultVo(
                memberIds.size(),
                deliveredIds.size(),
                skippedProtected,
                skippedMissing,
                deliveredIds
        );
    }

    private OrgAuthenticationSecurityMemberVo toMemberVo(
            OrgMember member,
            Map<Long, TwoFactorStats> statsMap,
            Map<Long, Integer> sessionCountMap,
            Map<Long, LocalDateTime> reminderMap,
            OrgPolicyService.OrgPolicySnapshot policy
    ) {
        TwoFactorStats stats = statsMap.getOrDefault(member.getUserId(), TwoFactorStats.empty());
        boolean twoFactorEnabled = stats.entryCount() > 0;
        OrgPolicyService.TwoFactorAccessState accessState = orgPolicyService.evaluateTwoFactorAccessState(member, policy, twoFactorEnabled);
        return new OrgAuthenticationSecurityMemberVo(
                String.valueOf(member.getId()),
                member.getUserId() == null ? null : String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                twoFactorEnabled,
                stats.entryCount(),
                sessionCountMap.getOrDefault(member.getUserId(), 0),
                stats.lastAuthenticatorAt(),
                reminderMap.get(member.getId()),
                accessState.inGracePeriod(),
                accessState.gracePeriodEndsAt(),
                accessState.blockedByPolicy()
        );
    }

    private boolean matchesFilter(OrgAuthenticationSecurityMemberVo row, String memberEmail, boolean onlyWithoutTwoFactor) {
        if (onlyWithoutTwoFactor && row.twoFactorEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(memberEmail)) {
            return true;
        }
        return row.memberEmail().toLowerCase(Locale.ROOT).contains(memberEmail.trim().toLowerCase(Locale.ROOT));
    }

    private Map<Long, TwoFactorStats> loadTwoFactorStats(List<OrgMember> members) {
        Set<Long> userIds = members.stream()
                .map(OrgMember::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, TwoFactorStats> statsMap = new LinkedHashMap<>();
        List<AuthenticatorEntry> entries = authenticatorEntryMapper.selectList(new LambdaQueryWrapper<AuthenticatorEntry>()
                .in(AuthenticatorEntry::getOwnerId, userIds)
                .orderByDesc(AuthenticatorEntry::getUpdatedAt));
        for (AuthenticatorEntry entry : entries) {
            statsMap.compute(entry.getOwnerId(), (userId, current) -> mergeStats(current, entry.getUpdatedAt()));
        }
        return statsMap;
    }

    private TwoFactorStats mergeStats(TwoFactorStats current, LocalDateTime updatedAt) {
        if (current == null) {
            return new TwoFactorStats(1, updatedAt);
        }
        LocalDateTime lastAuthenticatorAt = current.lastAuthenticatorAt();
        if (updatedAt != null && (lastAuthenticatorAt == null || updatedAt.isAfter(lastAuthenticatorAt))) {
            lastAuthenticatorAt = updatedAt;
        }
        return new TwoFactorStats(current.entryCount() + 1, lastAuthenticatorAt);
    }

    private Map<Long, Integer> loadActiveSessionCounts(List<OrgMember> members) {
        Set<Long> userIds = members.stream()
                .map(OrgMember::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        List<UserSession> sessions = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                .in(UserSession::getOwnerId, userIds)
                .eq(UserSession::getRevoked, 0)
                .gt(UserSession::getExpiresAt, LocalDateTime.now()));
        for (UserSession session : sessions) {
            counts.merge(session.getOwnerId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, LocalDateTime> loadLatestReminderMap(Long orgId) {
        List<OrgAuditEventVo> events = auditService.listByOrg(orgId, REMINDER_AUDIT_LIMIT, EVENT_REMINDER_SENT, null, null);
        Map<Long, LocalDateTime> reminderMap = new LinkedHashMap<>();
        for (OrgAuditEventVo event : events) {
            Long memberId = extractLongToken(event.detail(), "memberId=");
            if (memberId == null || reminderMap.containsKey(memberId)) {
                continue;
            }
            reminderMap.put(memberId, event.createdAt());
        }
        return reminderMap;
    }

    private Long extractLongToken(String detail, String prefix) {
        if (!StringUtils.hasText(detail) || !detail.contains(prefix)) {
            return null;
        }
        String[] tokens = detail.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.startsWith(prefix)) {
                continue;
            }
            try {
                return Long.parseLong(trimmed.substring(prefix.length()));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Map<Long, OrgMember> loadTargetMembers(Long orgId, List<Long> memberIds) {
        Map<Long, OrgMember> memberMap = new LinkedHashMap<>();
        for (Long memberId : memberIds) {
            try {
                OrgMember member = orgAccessService.loadActiveMemberById(orgId, memberId);
                memberMap.put(memberId, member);
            } catch (BizException exception) {
                if (exception.getCode() != ErrorCode.ORG_MEMBER_NOT_FOUND.getCode()) {
                    throw exception;
                }
            }
        }
        return memberMap;
    }

    private List<Long> normalizeMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "memberIds cannot be empty");
        }
        return memberIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private boolean isTwoFactorEnabled(Map<Long, TwoFactorStats> statsMap, Long userId) {
        return userId != null && statsMap.getOrDefault(userId, TwoFactorStats.empty()).entryCount() > 0;
    }

    private boolean isManager(OrgMember member) {
        return OrgAccessService.ROLE_OWNER.equals(member.getRole()) || OrgAccessService.ROLE_ADMIN.equals(member.getRole());
    }

    private String buildViewAuditDetail(Long orgId, String memberEmail, Boolean onlyWithoutTwoFactor, int count) {
        String normalizedEmail = StringUtils.hasText(memberEmail) ? memberEmail.trim().toLowerCase(Locale.ROOT) : "";
        return "orgId=" + orgId
                + ",memberEmail=" + normalizedEmail
                + ",onlyWithoutTwoFactor=" + Boolean.TRUE.equals(onlyWithoutTwoFactor)
                + ",count=" + count;
    }

    private String buildReminderAuditDetail(
            Long orgId,
            OrgMember actor,
            OrgMember target,
            OrgPolicyService.OrgPolicySnapshot policy
    ) {
        return "orgId=" + orgId
                + ",memberId=" + target.getId()
                + ",targetEmail=" + target.getUserEmail()
                + ",targetRole=" + target.getRole()
                + ",actorRole=" + actor.getRole()
                + ",enforcement=" + policy.twoFactorEnforcementLevel()
                + ",gracePeriodDays=" + policy.twoFactorGracePeriodDays();
    }

    private record TwoFactorStats(int entryCount, LocalDateTime lastAuthenticatorAt) {
        private static TwoFactorStats empty() {
            return new TwoFactorStats(0, null);
        }
    }
}
