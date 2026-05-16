package com.mmmail.server.commercial.oidc;

import java.util.Set;

public record OidcConfigVo(
        Long orgId,
        boolean enabled,
        String issuerUri,
        String clientId,
        String clientSecretRef,
        String callbackUri,
        Set<String> scopes,
        Set<String> allowedPostLoginRedirectUris
) {

    public static OidcConfigVo from(OidcClientConfig config) {
        return new OidcConfigVo(
                config.orgId(),
                config.enabled(),
                config.issuerUri(),
                config.clientId(),
                config.clientSecretRef(),
                config.callbackUri(),
                config.scopes(),
                config.allowedPostLoginRedirectUris()
        );
    }
}
