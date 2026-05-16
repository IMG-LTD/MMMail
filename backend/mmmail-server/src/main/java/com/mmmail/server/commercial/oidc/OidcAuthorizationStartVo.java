package com.mmmail.server.commercial.oidc;

import java.time.Instant;

public record OidcAuthorizationStartVo(String authorizationUrl, String state, Instant expiresAt) {
}
