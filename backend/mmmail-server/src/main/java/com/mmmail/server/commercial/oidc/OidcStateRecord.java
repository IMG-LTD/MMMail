package com.mmmail.server.commercial.oidc;

import java.time.Instant;

public record OidcStateRecord(
        String state,
        Long orgId,
        String nonce,
        String codeVerifier,
        String callbackUri,
        String postLoginRedirectUri,
        Instant expiresAt,
        Instant consumedAt
) {

    public OidcStateRecord markConsumed(Instant consumedAt) {
        return new OidcStateRecord(
                state,
                orgId,
                nonce,
                codeVerifier,
                callbackUri,
                postLoginRedirectUri,
                expiresAt,
                consumedAt
        );
    }
}
