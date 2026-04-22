package com.mmmail.orggovernance.scope;

import java.util.Set;

public record OrgScopeAccessDecision(Long activeOrgId, Set<String> visibleProductCodes) {

    public static OrgScopeAccessDecision personal() {
        return new OrgScopeAccessDecision(null, Set.of());
    }

    public boolean active() {
        return activeOrgId != null;
    }

    public boolean allows(String productCode) {
        if (visibleProductCodes == null || visibleProductCodes.isEmpty() || productCode == null || productCode.isBlank()) {
            return true;
        }
        return visibleProductCodes.contains(productCode.trim().toUpperCase());
    }
}
