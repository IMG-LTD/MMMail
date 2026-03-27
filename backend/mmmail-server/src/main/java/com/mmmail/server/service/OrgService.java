package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.BatchRemoveOrgMembersRequest;
import com.mmmail.server.model.dto.BatchUpdateOrgMemberRoleRequest;
import com.mmmail.server.model.dto.CreateOrgRequest;
import com.mmmail.server.model.dto.InviteOrgMemberRequest;
import com.mmmail.server.model.dto.RespondOrgInviteRequest;
import com.mmmail.server.model.dto.UpdateOrgMemberRoleRequest;
import com.mmmail.server.model.dto.UpdateOrgPolicyRequest;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.OrgBatchActionResultVo;
import com.mmmail.server.model.vo.OrgBatchFailureVo;
import com.mmmail.server.model.vo.OrgIncomingInviteVo;
import com.mmmail.server.model.vo.OrgMemberVo;
import com.mmmail.server.model.vo.OrgPolicyVo;
import com.mmmail.server.model.vo.OrgWorkspaceVo;
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
public class OrgService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";

    private static final String STATUS_INVITED = "INVITED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DECLINED = "DECLINED";

    private static final String POLICY_ALLOWED_EMAIL_DOMAINS = "allowedEmailDomains";
    private static final String POLICY_MEMBER_LIMIT = "memberLimit";
    private static final String POLICY_ADMIN_CAN_INVITE_ADMIN = "adminCanInviteAdmin";
    private static final String POLICY_ADMIN_CAN_REMOVE_ADMIN = "adminCanRemoveAdmin";
    private static final String POLICY_ADMIN_CAN_REVIEW_GOVERNANCE = "adminCanReviewGovernance";
    private static final String POLICY_ADMIN_CAN_EXECUTE_GOVERNANCE = "adminCanExecuteGovernance";
    private static final String POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE = "requireDualReviewGovernance";
    private static final String POLICY_GOVERNANCE_REVIEW_SLA_HOURS = "governanceReviewSlaHours";

    private static final int DEFAULT_MEMBER_LIMIT = 200;
    private static final int DEFAULT_GOVERNANCE_REVIEW_SLA_HOURS = 24;

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final OrgPolicyService orgPolicyService;

    public OrgService(
            OrgWorkspaceMapper orgWorkspaceMapper,
            OrgMemberMapper orgMemberMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            OrgPolicyService orgPolicyService
    ) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.orgPolicyService = orgPolicyService;
    }

    @Transactional
    public OrgWorkspaceVo createOrg(Long userId, CreateOrgRequest request, String ipAddress) {
        UserAccount user = loadUser(userId);
        String name = normalizeName(request.name());
        LocalDateTime now = LocalDateTime.now();

        OrgWorkspace org = new OrgWorkspace();
        org.setOwnerId(userId);
        org.setName(name);
        org.setSlug(generateUniqueSlug(name));
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        org.setDeleted(0);
        orgWorkspaceMapper.insert(org);

        OrgMember ownerMember = new OrgMember();
        ownerMember.setOrgId(org.getId());
        ownerMember.setUserId(userId);
        ownerMember.setUserEmail(user.getEmail());
        ownerMember.setRole(ROLE_OWNER);
        ownerMember.setStatus(STATUS_ACTIVE);
        ownerMember.setInvitedBy(userId);
        ownerMember.setJoinedAt(now);
        ownerMember.setCreatedAt(now);
        ownerMember.setUpdatedAt(now);
        ownerMember.setDeleted(0);
        orgMemberMapper.insert(ownerMember);

        auditService.record(userId, "ORG_CREATE", "orgId=" + org.getId(), ipAddress, org.getId());
        return toOrgWorkspaceVo(org, ownerMember, 1);
    }

    public List<OrgWorkspaceVo> listOrgs(Long userId, String ipAddress) {
        List<OrgMember> memberships = orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getUserId, userId)
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .orderByDesc(OrgMember::getUpdatedAt));

        if (memberships.isEmpty()) {
            auditService.record(userId, "ORG_LIST", "count=0", ipAddress);
            return List.of();
        }

        Set<Long> orgIds = memberships.stream()
                .map(OrgMember::getOrgId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Long, OrgWorkspace> orgMap = loadOrgMap(orgIds);
        Map<Long, Integer> activeCountByOrg = countActiveMembers(orgIds);

        List<OrgWorkspaceVo> result = memberships.stream()
                .map(member -> {
                    OrgWorkspace org = orgMap.get(member.getOrgId());
                    if (org == null) {
                        return null;
                    }
                    return toOrgWorkspaceVo(org, member, activeCountByOrg.getOrDefault(org.getId(), 0));
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        auditService.record(userId, "ORG_LIST", "count=" + result.size(), ipAddress);
        return result;
    }

    public List<OrgMemberVo> listMembers(Long userId, Long orgId, String ipAddress) {
        requireActiveMembership(userId, orgId);

        List<OrgMemberVo> result = orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrgId, orgId)
                        .orderByDesc(OrgMember::getUpdatedAt)
                        .orderByAsc(OrgMember::getUserEmail))
                .stream()
                .map(this::toOrgMemberVo)
                .toList();

        auditService.record(userId, "ORG_MEMBER_LIST", "orgId=" + orgId + ",count=" + result.size(), ipAddress, orgId);
        return result;
    }

    public OrgPolicyVo getPolicy(Long userId, Long orgId, String ipAddress) {
        return orgPolicyService.getPolicy(userId, orgId, ipAddress);
    }

    public List<Long> listGovernanceManagedOrgIds(Long userId) {
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getUserId, userId)
                        .eq(OrgMember::getStatus, STATUS_ACTIVE)
                        .in(OrgMember::getRole, List.of(ROLE_OWNER, ROLE_ADMIN)))
                .stream()
                .map(OrgMember::getOrgId)
                .distinct()
                .toList();
    }

    public OrgGovernanceAccess resolveGovernanceAccess(Long userId, Long orgId) {
        OrgMember member = requireActiveMembership(userId, orgId);
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);
        return new OrgGovernanceAccess(
                member.getRole(),
                policy.adminCanReviewGovernance(),
                policy.adminCanExecuteGovernance(),
                policy.requireDualReviewGovernance(),
                policy.governanceReviewSlaHours()
        );
    }

    @Transactional
    public OrgPolicyVo updatePolicy(Long userId, Long orgId, UpdateOrgPolicyRequest request, String ipAddress) {
        return orgPolicyService.updatePolicy(userId, orgId, request, ipAddress);
    }

    @Transactional
    public OrgMemberVo updateMemberRole(
            Long userId,
            Long orgId,
            Long memberId,
            UpdateOrgMemberRoleRequest request,
            String ipAddress
    ) {
        OrgMember actorMember = requireActiveMembership(userId, orgId);
        if (!ROLE_OWNER.equals(actorMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER can update member role");
        }

        OrgMember targetMember = loadOrgMember(orgId, memberId);
        if (ROLE_OWNER.equals(targetMember.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot update OWNER role");
        }

        String newRole = normalizeRole(request.role());
        String oldRole = targetMember.getRole();
        if (oldRole.equals(newRole)) {
            return toOrgMemberVo(targetMember);
        }

        targetMember.setRole(newRole);
        targetMember.setUpdatedAt(LocalDateTime.now());
        orgMemberMapper.updateById(targetMember);

        auditService.record(
                userId,
                "ORG_MEMBER_ROLE_UPDATE",
                "orgId=" + orgId + ",memberId=" + memberId + ",fromRole=" + oldRole + ",toRole=" + newRole,
                ipAddress,
                orgId
        );
        return toOrgMemberVo(targetMember);
    }

    @Transactional
    public void removeMember(Long userId, Long orgId, Long memberId, String ipAddress) {
        OrgMember actorMember = requireActiveMembership(userId, orgId);
        OrgMember targetMember = loadOrgMember(orgId, memberId);
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);

        assertCanRemoveMember(actorMember, targetMember, userId, policy);

        orgMemberMapper.deleteById(targetMember.getId());
        auditService.record(
                userId,
                "ORG_MEMBER_REMOVE",
                "orgId=" + orgId + ",memberId=" + memberId + ",target=" + targetMember.getUserEmail(),
                ipAddress,
                orgId
        );
    }

    @Transactional
    public OrgBatchActionResultVo batchUpdateMemberRole(
            Long userId,
            Long orgId,
            BatchUpdateOrgMemberRoleRequest request,
            String ipAddress
    ) {
        OrgMember actorMember = requireActiveMembership(userId, orgId);
        if (!ROLE_OWNER.equals(actorMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER can batch update member role");
        }

        List<Long> memberIds = normalizeMemberIds(request.memberIds());
        String targetRole = normalizeRole(request.role());

        List<String> successIds = new java.util.ArrayList<>();
        List<OrgBatchFailureVo> failedItems = new java.util.ArrayList<>();

        for (Long memberId : memberIds) {
            try {
                OrgMember member = loadOrgMember(orgId, memberId);
                if (ROLE_OWNER.equals(member.getRole())) {
                    throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot update OWNER role");
                }
                member.setRole(targetRole);
                member.setUpdatedAt(LocalDateTime.now());
                orgMemberMapper.updateById(member);
                successIds.add(String.valueOf(memberId));
            } catch (BizException ex) {
                failedItems.add(new OrgBatchFailureVo(String.valueOf(memberId), ex.getMessage()));
            }
        }

        auditService.record(
                userId,
                "ORG_MEMBER_BATCH_ROLE_UPDATE",
                "orgId=" + orgId + ",requested=" + memberIds.size() + ",success=" + successIds.size() + ",failed=" + failedItems.size(),
                ipAddress,
                orgId
        );

        return new OrgBatchActionResultVo(memberIds.size(), successIds, failedItems);
    }

    @Transactional
    public OrgBatchActionResultVo batchRemoveMembers(
            Long userId,
            Long orgId,
            BatchRemoveOrgMembersRequest request,
            String ipAddress
    ) {
        OrgMember actorMember = requireActiveMembership(userId, orgId);
        if (!ROLE_OWNER.equals(actorMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER can batch remove members");
        }

        List<Long> memberIds = normalizeMemberIds(request.memberIds());
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);

        List<String> successIds = new java.util.ArrayList<>();
        List<OrgBatchFailureVo> failedItems = new java.util.ArrayList<>();

        for (Long memberId : memberIds) {
            try {
                OrgMember member = loadOrgMember(orgId, memberId);
                assertCanRemoveMember(actorMember, member, userId, policy);
                orgMemberMapper.deleteById(member.getId());
                successIds.add(String.valueOf(memberId));
            } catch (BizException ex) {
                failedItems.add(new OrgBatchFailureVo(String.valueOf(memberId), ex.getMessage()));
            }
        }

        auditService.record(
                userId,
                "ORG_MEMBER_BATCH_REMOVE",
                "orgId=" + orgId + ",requested=" + memberIds.size() + ",success=" + successIds.size() + ",failed=" + failedItems.size(),
                ipAddress,
                orgId
        );

        return new OrgBatchActionResultVo(memberIds.size(), successIds, failedItems);
    }

    @Transactional
    public OrgMemberVo inviteMember(Long userId, Long orgId, InviteOrgMemberRequest request, String ipAddress) {
        OrgMember actorMember = requireActiveMembership(userId, orgId);
        if (!ROLE_OWNER.equals(actorMember.getRole()) && !ROLE_ADMIN.equals(actorMember.getRole())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER or ADMIN can invite members");
        }

        String targetEmail = normalizeEmail(request.email());
        if (targetEmail.equals(actorMember.getUserEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot invite yourself");
        }

        String targetRole = normalizeRole(request.role());
        OrgPolicyService.OrgPolicySnapshot policy = orgPolicyService.loadPolicySnapshot(orgId);

        if (ROLE_ADMIN.equals(actorMember.getRole()) && ROLE_ADMIN.equals(targetRole) && !policy.adminCanInviteAdmin()) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Admin cannot invite ADMIN by organization policy");
        }

        if (!policy.allowedEmailDomains().isEmpty()) {
            String emailDomain = extractEmailDomain(targetEmail);
            if (!policy.allowedEmailDomains().contains(emailDomain)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email domain is blocked by organization policy");
            }
        }

        UserAccount targetUser = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, targetEmail));
        if (targetUser == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "Target user not found");
        }

        OrgMember existing = orgMemberMapper.selectAnyByOrgAndEmail(orgId, targetEmail);
        boolean existingActiveOrInvited = existing != null
                && existing.getDeleted() != null
                && existing.getDeleted() == 0
                && (STATUS_ACTIVE.equals(existing.getStatus()) || STATUS_INVITED.equals(existing.getStatus()));
        if (existingActiveOrInvited) {
            throw new BizException(ErrorCode.ORG_INVITE_CONFLICT, "Member already active or invited");
        }

        long currentMemberCount = countGovernedMembers(orgId);
        if (currentMemberCount >= policy.memberLimit()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Organization member limit reached");
        }

        LocalDateTime now = LocalDateTime.now();
        OrgMember invite = existing;
        if (invite == null) {
            invite = new OrgMember();
            invite.setOrgId(orgId);
            invite.setUserEmail(targetEmail);
            invite.setCreatedAt(now);
            invite.setDeleted(0);
        }

        invite.setUserId(targetUser.getId());
        invite.setRole(targetRole);
        invite.setStatus(STATUS_INVITED);
        invite.setInvitedBy(userId);
        invite.setJoinedAt(null);
        invite.setUpdatedAt(now);
        invite.setDeleted(0);

        if (invite.getId() == null) {
            orgMemberMapper.insert(invite);
        } else {
            orgMemberMapper.updateIncludingDeleted(invite);
        }

        auditService.record(
                userId,
                "ORG_MEMBER_INVITE",
                "orgId=" + orgId + ",target=" + targetEmail + ",role=" + invite.getRole(),
                ipAddress,
                orgId
        );
        return toOrgMemberVo(invite);
    }

    public List<OrgIncomingInviteVo> listIncomingInvites(Long userId, String ipAddress) {
        UserAccount user = loadUser(userId);
        List<OrgMember> invites = orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getStatus, STATUS_INVITED)
                .and(wrapper -> wrapper.eq(OrgMember::getUserId, userId)
                        .or()
                        .eq(OrgMember::getUserEmail, user.getEmail()))
                .orderByDesc(OrgMember::getUpdatedAt));

        if (invites.isEmpty()) {
            auditService.record(userId, "ORG_INVITE_INCOMING", "count=0", ipAddress);
            return List.of();
        }

        Set<Long> orgIds = invites.stream()
                .map(OrgMember::getOrgId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Long, OrgWorkspace> orgMap = loadOrgMap(orgIds);
        Map<Long, String> inviterEmailMap = loadUserEmailMap(invites.stream()
                .map(OrgMember::getInvitedBy)
                .collect(java.util.stream.Collectors.toSet()));

        List<OrgIncomingInviteVo> result = invites.stream()
                .map(invite -> toIncomingInviteVo(invite, orgMap.get(invite.getOrgId()), inviterEmailMap.get(invite.getInvitedBy())))
                .filter(java.util.Objects::nonNull)
                .toList();

        auditService.record(userId, "ORG_INVITE_INCOMING", "count=" + result.size(), ipAddress);
        return result;
    }

    @Transactional
    public OrgIncomingInviteVo respondInvite(Long userId, Long inviteId, RespondOrgInviteRequest request, String ipAddress) {
        UserAccount user = loadUser(userId);
        OrgMember invite = orgMemberMapper.selectById(inviteId);
        if (invite == null || !STATUS_INVITED.equals(invite.getStatus())) {
            throw new BizException(ErrorCode.ORG_INVITE_INVALID, "Invite is not available");
        }

        boolean matched = (invite.getUserId() != null && invite.getUserId().equals(userId))
                || user.getEmail().equals(invite.getUserEmail());
        if (!matched) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "Cannot respond invite for another user");
        }

        LocalDateTime now = LocalDateTime.now();
        String response = request.response().trim().toUpperCase(Locale.ROOT);
        if ("ACCEPT".equals(response)) {
            invite.setStatus(STATUS_ACTIVE);
            invite.setUserId(userId);
            invite.setJoinedAt(now);
        } else if ("DECLINE".equals(response)) {
            invite.setStatus(STATUS_DECLINED);
            invite.setUserId(userId);
            invite.setJoinedAt(null);
        } else {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported invite response");
        }
        invite.setUpdatedAt(now);
        orgMemberMapper.updateById(invite);

        OrgWorkspace org = orgWorkspaceMapper.selectById(invite.getOrgId());
        String inviterEmail = resolveUserEmail(invite.getInvitedBy());
        auditService.record(
                userId,
                "ORG_INVITE_RESPOND",
                "inviteId=" + inviteId + ",orgId=" + invite.getOrgId() + ",response=" + response,
                ipAddress,
                invite.getOrgId()
        );
        return toIncomingInviteVo(invite, org, inviterEmail);
    }

    public List<OrgAuditEventVo> listOrgAuditEvents(
            Long userId,
            Long orgId,
            Integer limit,
            String eventType,
            String actorEmail,
            String keyword,
            String ipAddress
    ) {
        requireActiveMembership(userId, orgId);
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        List<OrgAuditEventVo> events = auditService.listByOrg(orgId, safeLimit, eventType, actorEmail, keyword);
        auditService.record(
                userId,
                "ORG_AUDIT_LIST",
                "orgId=" + orgId + ",count=" + events.size(),
                ipAddress,
                orgId
        );
        return events;
    }

    private OrgMember requireActiveMembership(Long userId, Long orgId) {
        OrgWorkspace org = orgWorkspaceMapper.selectById(orgId);
        if (org == null) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND);
        }

        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId));
        if (member == null || !STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No access to this organization");
        }
        return member;
    }

    private OrgMember loadOrgMember(Long orgId, Long memberId) {
        OrgMember member = orgMemberMapper.selectById(memberId);
        if (member == null || !orgId.equals(member.getOrgId())) {
            throw new BizException(ErrorCode.ORG_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private UserAccount loadUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
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
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                        .in(UserAccount::getId, userIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, UserAccount::getEmail));
    }

    private Map<Long, OrgWorkspace> loadOrgMap(Set<Long> orgIds) {
        if (orgIds.isEmpty()) {
            return Map.of();
        }
        return orgWorkspaceMapper.selectList(new LambdaQueryWrapper<OrgWorkspace>()
                        .in(OrgWorkspace::getId, orgIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(OrgWorkspace::getId, org -> org));
    }

    private Map<Long, Integer> countActiveMembers(Set<Long> orgIds) {
        if (orgIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (OrgMember member : orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .in(OrgMember::getOrgId, orgIds)
                .eq(OrgMember::getStatus, STATUS_ACTIVE))) {
            counts.merge(member.getOrgId(), 1, Integer::sum);
        }
        return counts;
    }

    private long countGovernedMembers(Long orgId) {
        Long count = orgMemberMapper.selectCount(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .in(OrgMember::getStatus, List.of(STATUS_ACTIVE, STATUS_INVITED)));
        return count == null ? 0L : count;
    }

    private void assertCanRemoveMember(
            OrgMember actorMember,
            OrgMember targetMember,
            Long actorUserId,
            OrgPolicyService.OrgPolicySnapshot policy
    ) {
        if (ROLE_OWNER.equals(targetMember.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot remove OWNER");
        }

        if (targetMember.getUserId() != null && targetMember.getUserId().equals(actorUserId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Use leave flow to quit organization");
        }

        if (ROLE_OWNER.equals(actorMember.getRole())) {
            return;
        }

        if (ROLE_ADMIN.equals(actorMember.getRole())) {
            if (ROLE_MEMBER.equals(targetMember.getRole())) {
                return;
            }
            if (ROLE_ADMIN.equals(targetMember.getRole()) && policy.adminCanRemoveAdmin()) {
                return;
            }
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "ADMIN cannot remove this role by organization policy");
        }

        throw new BizException(ErrorCode.ORG_FORBIDDEN, "No permission to remove member");
    }

    private List<Long> normalizeMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "memberIds cannot be empty");
        }
        if (memberIds.size() > 50) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "memberIds cannot exceed 50");
        }
        return memberIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private String extractEmailDomain(String email) {
        int index = email.lastIndexOf('@');
        if (index < 0 || index == email.length() - 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid email");
        }
        return email.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private OrgWorkspaceVo toOrgWorkspaceVo(OrgWorkspace org, OrgMember member, int memberCount) {
        return new OrgWorkspaceVo(
                String.valueOf(org.getId()),
                org.getName(),
                org.getSlug(),
                member.getRole(),
                member.getStatus(),
                memberCount,
                org.getUpdatedAt()
        );
    }

    private OrgMemberVo toOrgMemberVo(OrgMember member) {
        return new OrgMemberVo(
                String.valueOf(member.getId()),
                String.valueOf(member.getOrgId()),
                member.getUserId() == null ? null : String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                member.getStatus(),
                member.getInvitedBy() == null ? null : String.valueOf(member.getInvitedBy()),
                member.getJoinedAt(),
                member.getUpdatedAt()
        );
    }

    private OrgIncomingInviteVo toIncomingInviteVo(OrgMember invite, OrgWorkspace org, String invitedByEmail) {
        if (org == null) {
            return null;
        }
        return new OrgIncomingInviteVo(
                String.valueOf(invite.getId()),
                String.valueOf(org.getId()),
                org.getName(),
                org.getSlug(),
                invite.getRole(),
                invite.getStatus(),
                invitedByEmail,
                invite.getUpdatedAt()
        );
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Organization name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Organization name too long");
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Role is required");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_ADMIN.equals(normalized) && !ROLE_MEMBER.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Role must be ADMIN or MEMBER");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (true) {
            final String current = candidate;
            OrgWorkspace existing = orgWorkspaceMapper.selectOne(new LambdaQueryWrapper<OrgWorkspace>()
                    .eq(OrgWorkspace::getSlug, current));
            if (existing == null) {
                return candidate;
            }
            candidate = base + "-" + suffix;
            suffix++;
        }
    }

    private String slugify(String name) {
        String slug = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(slug) ? slug : "org";
    }

    public record OrgGovernanceAccess(
            String role,
            boolean adminCanReviewGovernance,
            boolean adminCanExecuteGovernance,
            boolean requireDualReviewGovernance,
            int governanceReviewSlaHours
    ) {
    }
}
