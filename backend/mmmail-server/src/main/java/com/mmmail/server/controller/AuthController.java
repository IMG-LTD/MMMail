package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.LoginRequest;
import com.mmmail.server.model.dto.RefreshRequest;
import com.mmmail.server.model.dto.RegisterRequest;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.service.AuthService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String CSRF_HEADER_NAME = "X-MMMAIL-CSRF";

    private final AuthService authService;
    private final String refreshCookieName;
    private final String csrfCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long refreshCookieMaxAgeSeconds;

    public AuthController(
            AuthService authService,
            @Value("${mmmail.auth.refresh-cookie-name:MMMAIL_REFRESH_TOKEN}") String refreshCookieName,
            @Value("${mmmail.auth.csrf-cookie-name:MMMAIL_CSRF_TOKEN}") String csrfCookieName,
            @Value("${mmmail.auth.cookie-secure:false}") boolean cookieSecure,
            @Value("${mmmail.auth.cookie-same-site:Lax}") String cookieSameSite,
            @Value("${mmmail.refresh-token-expire-hours:168}") long refreshExpireHours
    ) {
        this.authService = authService;
        this.refreshCookieName = refreshCookieName;
        this.csrfCookieName = csrfCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.refreshCookieMaxAgeSeconds = Math.max(3600L, refreshExpireHours * 3600L);
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        attachAuthCookies(httpResponse, response.refreshToken(), generateCsrfToken());
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        attachAuthCookies(httpResponse, response.refreshToken(), generateCsrfToken());
        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String cookieToken = readCookie(httpRequest, refreshCookieName);
        String bodyToken = request == null ? null : request.refreshToken();
        String refreshToken = StringUtils.hasText(cookieToken) ? cookieToken : bodyToken;
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.SESSION_INVALID, "Refresh token is required");
        }
        if (StringUtils.hasText(cookieToken)) {
            verifyCsrf(httpRequest);
        }
        AuthResponse response = authService.refresh(refreshToken, httpRequest.getRemoteAddr());
        attachAuthCookies(httpResponse, response.refreshToken(), generateCsrfToken());
        return Result.success(response);
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutAll(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr());
        clearAuthCookies(httpResponse);
        return Result.success(null);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logoutCurrent(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                httpRequest.getRemoteAddr()
        );
        clearAuthCookies(httpResponse);
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

    private void verifyCsrf(HttpServletRequest request) {
        String csrfCookie = readCookie(request, csrfCookieName);
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
        if (!StringUtils.hasText(csrfCookie) || !csrfCookie.equals(csrfHeader)) {
            throw new BizException(ErrorCode.FORBIDDEN, "CSRF token is invalid");
        }
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void attachAuthCookies(HttpServletResponse response, String refreshToken, String csrfToken) {
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(refreshCookieMaxAgeSeconds))
                .build();
        ResponseCookie csrfCookie = ResponseCookie.from(csrfCookieName, csrfToken)
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(refreshCookieMaxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());
        response.addHeader("Set-Cookie", csrfCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ZERO)
                .build();
        ResponseCookie csrfCookie = ResponseCookie.from(csrfCookieName, "")
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());
        response.addHeader("Set-Cookie", csrfCookie.toString());
    }

    private String generateCsrfToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }
}
