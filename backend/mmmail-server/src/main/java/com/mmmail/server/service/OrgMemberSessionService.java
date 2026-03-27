package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.vo.OrgMemberSessionVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrgMemberSessionService {

    private static final int DEFAULT_LIMIT = 60;
    private static final int MAX_LIMIT = 200;
    private static final int BOOLEAN_FALSE = 0;
    private static final int BOOLEAN_TRUE = 1;

    private final OrgAccessService orgAccessService;
    private final UserSessionMapper userSessionMapper;
    private final AuditService auditService;

    public OrgMemberSessionService(
            OrgAccessService orgAccessService,
            UserSessionMapper userSessionMapper,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.userSessionMapper = userSessionMapper;
        this.auditService = auditService;
    }

    public List<OrgMemberSessionVo> listSessions(
            Long actorUserId,
            Long currentSessionId,
            Long orgId,
            String memberEmail,
            Integer limit,
            String ipAddress
    ) {
        OrgMember actor = orgAccessService.requireManageMember(actorUserId, orgId);
        Map<Long, OrgMember> memberByUserId = listEligibleMembers(orgId, memberEmail);
        if (memberByUserId.isEmpty()) {
            auditService.record(actorUserId, "ORG_MEMBER_SESSION_LIST", buildListAuditDetail(actor, 0, memberEmail), ipAddress, orgId);
            return List.of();
        }
        List<OrgMemberSessionVo> sessions = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                        .in(UserSession::getOwnerId, memberByUserId.keySet())
                        .eq(UserSession::getRevoked, BOOLEAN_FALSE)
                        .gt(UserSession::getExpiresAt, LocalDateTime.now())
                        .orderByDesc(UserSession::getCreatedAt)
                        .last("limit " + normalizeLimit(limit)))
                .stream()
                .map(session -> toVo(session, memberByUserId.get(session.getOwnerId()), currentSessionId))
                .toList();
        auditService.record(actorUserId, "ORG_MEMBER_SESSION_LIST", buildListAuditDetail(actor, sessions.size(), memberEmail), ipAddress, orgId);
        return sessions;
    }

    public void revokeSession(
            Long actorUserId,
            Long currentSessionId,
            Long orgId,
            Long sessionId,
            String ipAddress
    ) {
        OrgMember actor = orgAccessService.requireManageMember(actorUserId, orgId);
        UserSession target = loadActiveSession(sessionId);
        OrgMember targetMember = orgAccessService.requireActiveMember(target.getOwnerId(), orgId);
        if (currentSessionId != null && currentSessionId.equals(target.getId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Current session cannot be revoked from organization monitor");
        }
        revokeSessionById(target.getId());
        auditService.record(
                actorUserId,
                "ORG_MEMBER_SESSION_REVOKE",
                buildRevokeAuditDetail(actor, targetMember, target.getId()),
                ipAddress,
                orgId
        );
    }

    private Map<Long, OrgMember> listEligibleMembers(Long orgId, String memberEmail) {
        return orgAccessService.listActiveMembers(orgId).stream()
                .filter(member -> member.getUserId() != null)
                .filter(member -> matchesMemberEmail(member, memberEmail))
                .collect(Collectors.toMap(OrgMember::getUserId, member -> member, (left, right) -> left));
    }

    private boolean matchesMemberEmail(OrgMember member, String memberEmail) {
        if (!StringUtils.hasText(memberEmail)) {
            return true;
        }
        return member.getUserEmail() != null
                && member.getUserEmail().toLowerCase().contains(memberEmail.trim().toLowerCase());
    }

    private UserSession loadActiveSession(Long sessionId) {
        UserSession session = userSessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() != null && session.getDeleted() == BOOLEAN_TRUE) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        if (session.getRevoked() != null && session.getRevoked() == BOOLEAN_TRUE) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        return session;
    }

    private void revokeSessionById(Long sessionId) {
        userSessionMapper.update(
                null,
                new LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getId, sessionId)
                        .eq(UserSession::getRevoked, BOOLEAN_FALSE)
                        .set(UserSession::getRevoked, BOOLEAN_TRUE)
                        .set(UserSession::getUpdatedAt, LocalDateTime.now())
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private OrgMemberSessionVo toVo(UserSession session, OrgMember member, Long currentSessionId) {
        return new OrgMemberSessionVo(
                String.valueOf(session.getId()),
                String.valueOf(member.getId()),
                String.valueOf(member.getUserId()),
                member.getUserEmail(),
                member.getRole(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                currentSessionId != null && currentSessionId.equals(session.getId())
        );
    }

    private String buildListAuditDetail(OrgMember actor, int count, String memberEmail) {
        String emailFilter = StringUtils.hasText(memberEmail) ? memberEmail.trim().toLowerCase() : "ALL";
        return "actorRole=" + actor.getRole() + ",count=" + count + ",memberEmail=" + emailFilter;
    }

    private String buildRevokeAuditDetail(OrgMember actor, OrgMember targetMember, Long sessionId) {
        return "actorRole=" + actor.getRole()
                + ",targetMemberId=" + targetMember.getId()
                + ",targetEmail=" + targetMember.getUserEmail()
                + ",sessionId=" + sessionId;
    }
}
