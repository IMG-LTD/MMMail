package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record OrgAuthenticationSecurityVo(
        String orgId,
        String twoFactorEnforcementLevel,
        int twoFactorGracePeriodDays,
        int totalActiveMembers,
        int protectedMembers,
        int unprotectedMembers,
        int protectedManagerSeats,
        int unprotectedManagerSeats,
        int enforcementBlockedMembers,
        List<OrgAuthenticationSecurityMemberVo> members,
        LocalDateTime generatedAt
) {
}
