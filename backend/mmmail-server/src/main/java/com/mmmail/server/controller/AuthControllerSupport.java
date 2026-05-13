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
import org.springframework.util.StringUtils;

import java.util.List;

class AuthControllerSupport {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final String cookiePath;

    AuthControllerSupport(AuthService authService, AuthCookieService authCookieService, String cookiePath) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.cookiePath = cookiePath;
    }

    protected Result<AuthResponse> registerUser(
            RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), cookiePath);
        return Result.success(response);
    }

    protected Result<AuthResponse> loginUser(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), cookiePath);
        return Result.success(response);
    }

    protected Result<AuthResponse> refreshSession(
            RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        AuthResponse response = authService.refresh(refreshToken, httpRequest.getRemoteAddr());
        authCookieService.attachAuthCookies(httpResponse, response.refreshToken(), cookiePath);
        return Result.success(response);
    }

    protected Result<Void> logoutAllSessions(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutAll(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr());
        authCookieService.clearAuthCookies(httpResponse, cookiePath);
        return Result.success(null);
    }

    protected Result<Void> logoutCurrentSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutCurrent(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                httpRequest.getRemoteAddr()
        );
        authCookieService.clearAuthCookies(httpResponse, cookiePath);
        return Result.success(null);
    }

    protected Result<List<UserSessionVo>> listUserSessions() {
        return Result.success(authService.listSessions(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId()
        ));
    }

    protected Result<Void> revokeUserSession(Long sessionId, HttpServletRequest httpRequest) {
        authService.revokeSession(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentPrincipal().sessionId(),
                sessionId,
                httpRequest.getRemoteAddr()
        );
        return Result.success(null);
    }

    private String resolveRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        String cookieToken = authCookieService.readRefreshToken(httpRequest);
        String bodyToken = request == null ? null : request.refreshToken();
        String refreshToken = StringUtils.hasText(cookieToken) ? cookieToken : bodyToken;
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "Refresh token is required");
        }
        if (StringUtils.hasText(cookieToken)) {
            authCookieService.verifyCsrf(httpRequest);
        }
        return refreshToken;
    }
}
