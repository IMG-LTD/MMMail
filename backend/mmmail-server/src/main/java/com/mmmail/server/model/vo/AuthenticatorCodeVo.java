package com.mmmail.server.model.vo;

public record AuthenticatorCodeVo(
        String code,
        int expiresInSeconds,
        int periodSeconds,
        int digits
) {
}
