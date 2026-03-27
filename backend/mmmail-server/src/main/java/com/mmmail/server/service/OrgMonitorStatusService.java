package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import com.mmmail.server.model.vo.OrgMonitorStatusVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class OrgMonitorStatusService {

    private static final String VISIBILITY_SCOPE_ALL_ADMINS = "ALL_ADMINS";
    private static final String RETENTION_MODE_PERMANENT = "PERMANENT";
    private static final int BOOLEAN_FALSE = 0;

    private final OrgAccessService orgAccessService;
    private final AuditService auditService;
    private final UserSessionMapper userSessionMapper;
    private final OrgAuditQueryService orgAuditQueryService;

    public OrgMonitorStatusService(
            OrgAccessService orgAccessService,
            AuditService auditService,
            UserSessionMapper userSessionMapper,
            OrgAuditQueryService orgAuditQueryService
    ) {
        this.orgAccessService = orgAccessService;
        this.auditService = auditService;
        this.userSessionMapper = userSessionMapper;
        this.orgAuditQueryService = orgAuditQueryService;
    }

    public OrgMonitorStatusVo getStatus(Long userId, Long currentSessionId, Long orgId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        SessionCoverage coverage = summarizeSessions(orgId, currentSessionId);
        OrgAuditEventVo oldestEvent = auditService.firstOrgEvent(orgId);
        OrgAuditEventVo latestEvent = auditService.latestOrgEvent(orgId);
        OrgMonitorStatusVo status = new OrgMonitorStatusVo(
                String.valueOf(orgId),
                true,
                false,
                false,
                false,
                VISIBILITY_SCOPE_ALL_ADMINS,
                RETENTION_MODE_PERMANENT,
                auditService.countOrgEvents(orgId),
                auditService.countOrgEventTypes(orgId),
                coverage.activeSessions(),
                coverage.managerSessions(),
                coverage.protectedSessions(),
                orgAuditQueryService.maxLimit(),
                oldestEvent == null ? null : oldestEvent.createdAt(),
                latestEvent,
                LocalDateTime.now()
        );
        auditService.record(userId, "ORG_MONITOR_STATUS_VIEW", buildAuditDetail(status), ipAddress, orgId);
        return status;
    }

    private SessionCoverage summarizeSessions(Long orgId, Long currentSessionId) {
        Map<Long, String> roleByUserId = orgAccessService.listActiveMembers(orgId).stream()
                .filter(member -> member.getUserId() != null)
                .collect(Collectors.toMap(OrgMember::getUserId, OrgMember::getRole, keepFirst()));
        if (roleByUserId.isEmpty()) {
            return new SessionCoverage(0, 0, 0);
        }
        List<UserSession> sessions = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                .in(UserSession::getOwnerId, roleByUserId.keySet())
                .eq(UserSession::getRevoked, BOOLEAN_FALSE)
                .gt(UserSession::getExpiresAt, LocalDateTime.now()));
        int managerSessions = 0;
        int protectedSessions = 0;
        for (UserSession session : sessions) {
            if (orgAccessService.canManage(roleByUserId.get(session.getOwnerId()))) {
                managerSessions++;
            }
            if (currentSessionId != null && currentSessionId.equals(session.getId())) {
                protectedSessions++;
            }
        }
        return new SessionCoverage(sessions.size(), managerSessions, protectedSessions);
    }

    private BinaryOperator<String> keepFirst() {
        return (left, right) -> left;
    }

    private String buildAuditDetail(OrgMonitorStatusVo status) {
        return "orgId=" + status.orgId()
                + ",events=" + status.totalEvents()
                + ",retention=" + status.retentionMode()
                + ",visibility=" + status.visibilityScope();
    }

    private record SessionCoverage(
            int activeSessions,
            int managerSessions,
            int protectedSessions
    ) {
    }
}
