package com.mmmail.server.commercial.oidc;

import java.util.Set;

public record OidcClientConfig(
        Long orgId,
        boolean enabled,
        String issuerUri,
        String clientId,
        String clientSecretRef,
        String callbackUri,
        Set<String> scopes,
        Set<String> allowedPostLoginRedirectUris
) {
}
