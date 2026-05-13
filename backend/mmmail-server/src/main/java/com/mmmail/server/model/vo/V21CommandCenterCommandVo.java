package com.mmmail.server.model.vo;

public record V21CommandCenterCommandVo(
        String id,
        String name,
        String description,
        String product,
        boolean enabled,
        int parameterCount
) {
}
