package com.mmmail.server.commercial.oidc;

import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.service.AuthService;
import com.mmmail.server.service.SsoLoginRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceOidcSessionIssuer implements OidcSessionIssuer {

    private final AuthService authService;

    public AuthServiceOidcSessionIssuer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthResponse issue(OidcSessionRequest request) {
        return authService.loginSso(new SsoLoginRequest(
                request.email(),
                request.ipAddress(),
                request.subject()
        ));
    }
}
