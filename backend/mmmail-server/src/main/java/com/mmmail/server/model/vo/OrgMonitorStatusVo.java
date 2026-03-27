package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgMonitorStatusVo(
        String orgId,
        boolean alwaysOn,
        boolean canDisable,
        boolean canDeleteEvents,
        boolean canEditEvents,
        String visibilityScope,
        String retentionMode,
        int totalEvents,
        int coveredEventTypes,
        int activeSessions,
        int managerSessions,
        int protectedSessions,
        int maximumExportSize,
        LocalDateTime oldestEventAt,
        OrgAuditEventVo latestEvent,
        LocalDateTime generatedAt
) {
}
