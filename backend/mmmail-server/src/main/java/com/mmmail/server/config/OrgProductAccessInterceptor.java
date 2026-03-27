package com.mmmail.server.config;

import com.mmmail.server.service.AccountProductAccessGuardService;
import com.mmmail.server.service.OrgProductAccessGuardService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OrgProductAccessInterceptor implements HandlerInterceptor {

    private final AccountProductAccessGuardService accountProductAccessGuardService;
    private final OrgProductAccessGuardService orgProductAccessGuardService;

    public OrgProductAccessInterceptor(
            AccountProductAccessGuardService accountProductAccessGuardService,
            OrgProductAccessGuardService orgProductAccessGuardService
    ) {
        this.accountProductAccessGuardService = accountProductAccessGuardService;
        this.orgProductAccessGuardService = orgProductAccessGuardService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }
        Long userId = SecurityUtils.currentUserId();
        accountProductAccessGuardService.enforce(request, userId);
        orgProductAccessGuardService.enforce(request, userId);
        return true;
    }
}
