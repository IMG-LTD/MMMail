package com.mmmail.server.model.vo;

import java.util.List;

public record OrgAuthenticationSecurityReminderResultVo(
        int requestedCount,
        int deliveredCount,
        int skippedProtectedCount,
        int skippedMissingCount,
        List<String> deliveredMemberIds
) {
}
