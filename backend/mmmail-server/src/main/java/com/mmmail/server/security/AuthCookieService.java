package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthCookieService {

    public static final String V1_AUTH_COOKIE_PATH = "/api/v1/auth";
    public static final String V2_AUTH_COOKIE_PATH = "/api/v2/auth";

    private static final String CSRF_HEADER_NAME = "X-MMMAIL-CSRF";
    private static final String REFRESH_COOKIE_NAME_PROPERTY = "mmmail.auth.refresh-cookie-name";
    private static final String CSRF_COOKIE_NAME_PROPERTY = "mmmail.auth.csrf-cookie-name";
    private static final String COOKIE_SECURE_PROPERTY = "mmmail.auth.cookie-secure";
    private static final String COOKIE_SAME_SITE_PROPERTY = "mmmail.auth.cookie-same-site";
    private static final String REFRESH_EXPIRE_HOURS_PROPERTY = "mmmail.refresh-token-expire-hours";
    private static final String DEFAULT_REFRESH_COOKIE_NAME = "MMMAIL_REFRESH_TOKEN";
    private static final String DEFAULT_CSRF_COOKIE_NAME = "MMMAIL_CSRF_TOKEN";
    private static final String DEFAULT_COOKIE_SAME_SITE = "Lax";
    private static final long SECONDS_PER_HOUR = 3600L;
    private static final long MIN_REFRESH_COOKIE_MAX_AGE_SECONDS = SECONDS_PER_HOUR;

    private final String refreshCookieName;
    private final String csrfCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long refreshCookieMaxAgeSeconds;

    public AuthCookieService(Environment environment) {
        this.refreshCookieName = environment.getProperty(REFRESH_COOKIE_NAME_PROPERTY, DEFAULT_REFRESH_COOKIE_NAME);
        this.csrfCookieName = environment.getProperty(CSRF_COOKIE_NAME_PROPERTY, DEFAULT_CSRF_COOKIE_NAME);
        this.cookieSecure = environment.getProperty(COOKIE_SECURE_PROPERTY, Boolean.class, false);
        this.cookieSameSite = environment.getProperty(COOKIE_SAME_SITE_PROPERTY, DEFAULT_COOKIE_SAME_SITE);
        long refreshExpireHours = environment.getProperty(REFRESH_EXPIRE_HOURS_PROPERTY, Long.class, 168L);
        this.refreshCookieMaxAgeSeconds = Math.max(MIN_REFRESH_COOKIE_MAX_AGE_SECONDS, refreshExpireHours * SECONDS_PER_HOUR);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, refreshCookieName);
    }

    public void verifyCsrf(HttpServletRequest request) {
        String csrfCookie = readCookie(request, csrfCookieName);
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
        if (!StringUtils.hasText(csrfCookie) || !csrfCookie.equals(csrfHeader)) {
            throw new BizException(ErrorCode.FORBIDDEN, "CSRF token is invalid");
        }
    }

    public void attachAuthCookies(HttpServletResponse response, String refreshToken, String path) {
        Duration maxAge = Duration.ofSeconds(refreshCookieMaxAgeSeconds);
        response.addHeader("Set-Cookie", refreshCookie(refreshToken, path, maxAge).toString());
        response.addHeader("Set-Cookie", csrfCookie(generateCsrfToken(), maxAge).toString());
    }

    public void clearAuthCookies(HttpServletResponse response, String path) {
        response.addHeader("Set-Cookie", refreshCookie("", path, Duration.ZERO).toString());
        response.addHeader("Set-Cookie", csrfCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie refreshCookie(String value, String path, Duration maxAge) {
        return ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(path)
                .sameSite(cookieSameSite)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie csrfCookie(String value, Duration maxAge) {
        return ResponseCookie.from(csrfCookieName, value)
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(maxAge)
                .build();
    }

    private static String readCookie(HttpServletRequest request, String name) {
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

    private static String generateCsrfToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }
}
