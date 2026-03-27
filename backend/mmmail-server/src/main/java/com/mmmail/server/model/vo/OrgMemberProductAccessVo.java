package com.mmmail.server.model.vo;

import java.util.List;

public record OrgMemberProductAccessVo(
        String memberId,
        String userId,
        String userEmail,
        String role,
        boolean currentUser,
        int enabledProductCount,
        List<OrgProductAccessItemVo> products
) {
}
