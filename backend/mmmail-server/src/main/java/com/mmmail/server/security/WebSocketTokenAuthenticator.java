package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSocketTokenAuthenticator {

    private final JwtService jwtService;
    private final JwtPrincipalValidator jwtPrincipalValidator;

    public WebSocketTokenAuthenticator(JwtService jwtService, JwtPrincipalValidator jwtPrincipalValidator) {
        this.jwtService = jwtService;
        this.jwtPrincipalValidator = jwtPrincipalValidator;
    }

    public JwtPrincipal authenticate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "notification websocket token is required");
        }
        JwtPrincipal principal = jwtService.parseToken(token.trim());
        if (!jwtPrincipalValidator.isActive(principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "notification websocket token is inactive");
        }
        return principal;
    }
}
