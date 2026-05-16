package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.LoginRequest;
import com.mmmail.server.model.dto.RefreshRequest;
import com.mmmail.server.model.dto.RegisterRequest;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.AuthUserInfoVo;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.security.AuthCookieService;
import com.mmmail.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends AuthControllerSupport {

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        super(authService, authCookieService, AuthCookieService.V1_AUTH_COOKIE_PATH);
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return registerUser(request, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return loginUser(request, httpRequest, httpResponse);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return refreshSession(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout-all")
    public Result<Void> logoutAll(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return logoutAllSessions(httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return logoutCurrentSession(httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public Result<AuthUserInfoVo> me() {
        return currentUserInfo();
    }

    @GetMapping("/sessions")
    public Result<List<UserSessionVo>> sessions() {
        return listUserSessions();
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    public Result<Void> revokeSession(@PathVariable Long sessionId, HttpServletRequest httpRequest) {
        return revokeUserSession(sessionId, httpRequest);
    }
}
