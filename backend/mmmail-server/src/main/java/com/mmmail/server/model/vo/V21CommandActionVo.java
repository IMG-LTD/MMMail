package com.mmmail.server.model.vo;

import java.util.Map;

public record V21CommandActionVo(
        String kind,
        Map<String, Object> payload
) {
    public V21CommandActionVo {
        payload = Map.copyOf(payload);
    }
}
