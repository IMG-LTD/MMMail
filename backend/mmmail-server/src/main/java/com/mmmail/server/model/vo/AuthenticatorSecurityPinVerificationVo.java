package com.mmmail.server.model.vo;

public record AuthenticatorSecurityPinVerificationVo(
        boolean verified,
        int lockTimeoutSeconds
) {
}
