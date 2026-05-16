package com.mmmail.server.commercial.oidc;

public record OidcSessionRequest(String subject, String email, String ipAddress) {
}
