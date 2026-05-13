package com.mmmail.server.access;

import com.mmmail.platform.access.AccessEntitlement;

public interface V21ApiEntitlementProvider {

    boolean hasEntitlement(Long userId, AccessEntitlement entitlement);
}
