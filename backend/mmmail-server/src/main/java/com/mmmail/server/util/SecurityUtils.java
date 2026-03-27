package com.mmmail.server.util;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static JwtPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentPrincipal().userId();
    }

    public static Long currentSessionId() {
        return currentPrincipal().sessionId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentPrincipal().role());
    }
}
