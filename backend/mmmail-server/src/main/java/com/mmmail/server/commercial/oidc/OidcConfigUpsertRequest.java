package com.mmmail.server.commercial.oidc;

import java.util.Set;

public record OidcConfigUpsertRequest(
        boolean enabled,
        String issuerUri,
        String clientId,
        String clientSecretRef,
        String callbackUri,
        Set<String> scopes,
        Set<String> allowedPostLoginRedirectUris
) {

    public OidcClientConfig toConfig(Long orgId) {
        return new OidcClientConfig(
                orgId,
                enabled,
                issuerUri,
                clientId,
                clientSecretRef,
                callbackUri,
                scopes,
                allowedPostLoginRedirectUris
        );
    }
}
