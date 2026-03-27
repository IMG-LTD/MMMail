package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.dto.LoginRequest;
import com.mmmail.server.model.dto.RegisterRequest;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.UserSessionVo;
import com.mmmail.server.model.vo.UserProfileVo;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.JwtService;
import com.mmmail.server.security.SecurityRateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final UserSessionMapper userSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserPreferenceService userPreferenceService;
    private final SecurityRateLimitService securityRateLimitService;
    private final long refreshExpireHours;

    public AuthService(
            UserAccountMapper userAccountMapper,
            UserSessionMapper userSessionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            UserPreferenceService userPreferenceService,
            SecurityRateLimitService securityRateLimitService,
            @Value("${mmmail.refresh-token-expire-hours:168}") long refreshExpireHours
    ) {
        this.userAccountMapper = userAccountMapper;
        this.userSessionMapper = userSessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.userPreferenceService = userPreferenceService;
        this.securityRateLimitService = securityRateLimitService;
        this.refreshExpireHours = refreshExpireHours;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        UserAccount exists = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail, request.email())
        );
        if (exists != null) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole("USER");
        user.setStatus(1);
        user.setTokenVersion(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userAccountMapper.insert(user);

        auditService.record(user.getId(), "REGISTER", "User registered", ipAddress);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedIpAddress = normalizeIpAddress(ipAddress);
        securityRateLimitService.ensureLoginAllowed(normalizedEmail, normalizedIpAddress);
        UserAccount user = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail, normalizedEmail)
        );
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            securityRateLimitService.recordLoginFailure(normalizedEmail, normalizedIpAddress);
            auditService.record(user == null ? null : user.getId(), "LOGIN_FAILURE", "Invalid credentials email=" + normalizedEmail, normalizedIpAddress);
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            securityRateLimitService.recordLoginFailure(normalizedEmail, normalizedIpAddress);
            auditService.record(user.getId(), "LOGIN_BLOCKED", "Disabled user login blocked", normalizedIpAddress);
            throw new BizException(ErrorCode.FORBIDDEN, "User is disabled");
        }

        securityRateLimitService.resetLoginFailures(normalizedEmail, normalizedIpAddress);
        auditService.record(user.getId(), "LOGIN_SUCCESS", "User login success", normalizedIpAddress);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String ipAddress) {
        UserSession session = findActiveSession(refreshToken);
        UserAccount user = userAccountMapper.selectById(session.getOwnerId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            revokeSessionById(session.getId());
            throw new BizException(ErrorCode.SESSION_INVALID);
        }

        revokeSessionById(session.getId());
        auditService.record(user.getId(), "TOKEN_REFRESH", "Refresh token rotated", ipAddress);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logoutAll(Long userId, String ipAddress) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        int nextVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1;
        userAccountMapper.update(
                null,
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getId, userId)
                        .set(UserAccount::getTokenVersion, nextVersion)
                        .set(UserAccount::getUpdatedAt, LocalDateTime.now())
        );
        revokeAllSessions(userId);
        auditService.record(userId, "LOGOUT_ALL", "All devices signed out", ipAddress);
    }

    @Transactional
    public void logoutCurrent(Long userId, Long sessionId, String ipAddress) {
        UserSession session = userSessionMapper.selectOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getId, sessionId)
                .eq(UserSession::getOwnerId, userId)
                .last("limit 1"));
        if (session == null) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        revokeSessionById(sessionId);
        auditService.record(userId, "LOGOUT", "Current device signed out", ipAddress);
    }

    public List<UserSessionVo> listSessions(Long userId, Long currentSessionId) {
        LocalDateTime now = LocalDateTime.now();
        return userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                        .eq(UserSession::getOwnerId, userId)
                        .orderByDesc(UserSession::getCreatedAt))
                .stream()
                .filter(session -> session.getRevoked() != null && session.getRevoked() == 0)
                .filter(session -> session.getExpiresAt() != null && session.getExpiresAt().isAfter(now))
                .map(session -> new UserSessionVo(
                        String.valueOf(session.getId()),
                        session.getCreatedAt(),
                        session.getExpiresAt(),
                        currentSessionId != null && currentSessionId.equals(session.getId())
                ))
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, Long currentSessionId, Long sessionId, String ipAddress) {
        UserSession target = userSessionMapper.selectOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getId, sessionId)
                .eq(UserSession::getOwnerId, userId));
        if (target == null) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        if (currentSessionId != null && currentSessionId.equals(sessionId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Current session cannot be revoked by this endpoint");
        }
        if (target.getRevoked() != null && target.getRevoked() == 1) {
            return;
        }
        revokeSessionById(sessionId);
        auditService.record(userId, "SESSION_REVOKE", "Session revoked id=" + sessionId, ipAddress);
    }

    private UserSession findActiveSession(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        UserSession session = userSessionMapper.selectOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getRefreshTokenHash, tokenHash)
                .eq(UserSession::getRevoked, 0));

        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.SESSION_INVALID);
        }
        return session;
    }

    private AuthResponse buildAuthResponse(UserAccount user) {
        SessionToken sessionToken = createRefreshSession(user.getId());
        JwtPrincipal principal = new JwtPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getTokenVersion(),
                sessionToken.sessionId()
        );
        String accessToken = jwtService.generateToken(principal);
        UserProfileVo profile = new UserProfileVo(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                userPreferenceService.resolveMailAddressMode(user.getId())
        );
        return new AuthResponse(accessToken, sessionToken.refreshToken(), profile);
    }

    private SessionToken createRefreshSession(Long userId) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        UserSession session = new UserSession();
        session.setOwnerId(userId);
        session.setRefreshTokenHash(hashToken(token));
        session.setExpiresAt(now.plusHours(refreshExpireHours));
        session.setRevoked(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setDeleted(0);
        userSessionMapper.insert(session);
        return new SessionToken(session.getId(), token);
    }

    private void revokeSessionById(Long sessionId) {
        userSessionMapper.update(
                null,
                new LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getId, sessionId)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1)
                        .set(UserSession::getUpdatedAt, LocalDateTime.now())
        );
    }

    private void revokeAllSessions(Long userId) {
        userSessionMapper.update(
                null,
                new LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getOwnerId, userId)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1)
                        .set(UserSession::getUpdatedAt, LocalDateTime.now())
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : "";
    }

    private String normalizeIpAddress(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress.trim() : "0.0.0.0";
    }

    private record SessionToken(Long sessionId, String refreshToken) {
    }
}
