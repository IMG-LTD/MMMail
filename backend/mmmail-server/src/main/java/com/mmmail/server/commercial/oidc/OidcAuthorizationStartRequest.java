package com.mmmail.server.commercial.oidc;

public record OidcAuthorizationStartRequest(Long orgId, String postLoginRedirectUri) {
}
