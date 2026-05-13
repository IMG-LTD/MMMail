package com.mmmail.server.access;

import com.mmmail.platform.access.AccessEntitlement;
import org.springframework.stereotype.Component;

@Component
public class CommunityV21ApiEntitlementProvider implements V21ApiEntitlementProvider {

    @Override
    public boolean hasEntitlement(Long userId, AccessEntitlement entitlement) {
        return AccessEntitlement.COMMUNITY == entitlement;
    }
}
