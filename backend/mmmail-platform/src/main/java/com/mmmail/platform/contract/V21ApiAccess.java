package com.mmmail.platform.contract;

import java.util.List;

public record V21ApiAccess(List<String> permissions, String entitlement) {

    public V21ApiAccess {
        permissions = List.copyOf(permissions);
    }
}
