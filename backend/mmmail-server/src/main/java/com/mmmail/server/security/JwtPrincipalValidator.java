package com.mmmail.server.security;

import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JwtPrincipalValidator {

    private final UserAccountMapper userAccountMapper;
    private final UserSessionMapper userSessionMapper;

    public JwtPrincipalValidator(UserAccountMapper userAccountMapper, UserSessionMapper userSessionMapper) {
        this.userAccountMapper = userAccountMapper;
        this.userSessionMapper = userSessionMapper;
    }

    public boolean isActive(JwtPrincipal principal) {
        UserAccount user = userAccountMapper.selectById(principal.userId());
        return user != null
                && principal.tokenVersion() != null
                && principal.tokenVersion().equals(user.getTokenVersion())
                && isSessionActive(principal);
    }

    private boolean isSessionActive(JwtPrincipal principal) {
        if (principal.sessionId() == null) {
            return false;
        }
        UserSession session = userSessionMapper.selectById(principal.sessionId());
        if (session == null || !principal.userId().equals(session.getOwnerId())) {
            return false;
        }
        if (session.getRevoked() != null && session.getRevoked() == 1) {
            return false;
        }
        return session.getExpiresAt() != null && session.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
