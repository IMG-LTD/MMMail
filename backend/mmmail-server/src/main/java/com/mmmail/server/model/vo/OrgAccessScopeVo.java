package com.mmmail.server.model.vo;

import java.util.List;

public record OrgAccessScopeVo(
        String orgId,
        String orgName,
        String orgSlug,
        String role,
        int enabledProductCount,
        List<OrgProductAccessItemVo> products
) {
}
