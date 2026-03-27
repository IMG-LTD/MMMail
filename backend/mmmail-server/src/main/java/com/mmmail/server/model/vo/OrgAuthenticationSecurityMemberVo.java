package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgAuthenticationSecurityMemberVo(
        String memberId,
        String userId,
        String memberEmail,
        String role,
        boolean twoFactorEnabled,
        int authenticatorEntryCount,
        int activeSessionCount,
        LocalDateTime lastAuthenticatorAt,
        LocalDateTime lastReminderAt,
        boolean inGracePeriod,
        LocalDateTime gracePeriodEndsAt,
        boolean blockedByPolicy
) {
}
