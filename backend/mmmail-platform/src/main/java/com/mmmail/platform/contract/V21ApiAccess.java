package com.mmmail.platform.contract;

import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessPermission;

import java.util.List;

public record V21ApiAccess(List<String> permissions, String entitlement) {

    public V21ApiAccess {
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("v2.1 API permissions are required");
        }
        permissions = permissions.stream()
                .map(AccessPermission::new)
                .map(AccessPermission::value)
                .toList();
        entitlement = AccessEntitlement.fromContractValue(entitlement).contractValue();
    }
}
