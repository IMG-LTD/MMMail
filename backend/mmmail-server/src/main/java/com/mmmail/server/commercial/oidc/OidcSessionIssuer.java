package com.mmmail.server.commercial.oidc;

import com.mmmail.server.model.vo.AuthResponse;

public interface OidcSessionIssuer {

    AuthResponse issue(OidcSessionRequest request);
}
