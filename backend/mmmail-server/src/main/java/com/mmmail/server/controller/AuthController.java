package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.LoginRequest;
import com.mmmail.server.model.dto.RefreshRequest;
import com.mmmail.server.model.dto.RegisterRequest;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.security.AuthCookieService;
import com.mmmail.server.service.AuthService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V1_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V1_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String cookieToken = authCookieService.readRefreshToken(httpRequest);
        String bodyToken = request == null ? null : request.refreshToken();
        String refreshToken = StringUtils.hasText(cookieToken) ? cookieToken : bodyToken;
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "Refresh token is required");
        }
        if (StringUtils.hasText(cookieToken)) {
            authCookieService.verifyCsrf(httpRequest);
        }
        AuthResponse response = authService.refresh(refreshToken, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), AuthCookieService.V1_AUTH_COOKIE_PATH);
        return Result.success(response);
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutAll(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr());
        authCookieService.clearAuthCookies(httpResponse, AuthCookieService.V1_AUTH_COOKIE_PATH);
        return Result.success(null);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutCurrent(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                httpRequest.getRemoteAddr()
        );
        authCookieService.clearAuthCookies(httpResponse, AuthCookieService.V1_AUTH_COOKIE_PATH);
        return Result.success(null);
    }

    @GetMapping("/sessions")
    public Result<List<UserSessionVo>> sessions() {
        return Result.success(authService.listSessions(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId()
        ));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    public Result<Void> revokeSession(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        authService.revokeSession(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                sessionId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }
}
