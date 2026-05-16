package com.mmmail.server.commercial.oidc;

import com.mmmail.server.model.vo.AuthResponse;

public record OidcCallbackResult(
        Long orgId,
        String state,
        String postLoginRedirectUri,
        String status,
        AuthResponse auth
) {
}
