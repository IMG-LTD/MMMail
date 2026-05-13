package com.mmmail.server.access;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.foundation.tenant.TenantScopeHeaders;
import com.mmmail.platform.access.AccessDecision;
import com.mmmail.platform.access.AccessRequest;
import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.server.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class V21ApiAccessGateInterceptor implements HandlerInterceptor {

    private final V21ApiContractMatcher contractMatcher;
    private final V21ApiAccessGateService accessGateService;

    public V21ApiAccessGateInterceptor(
            V21ApiContractMatcher contractMatcher,
            V21ApiAccessGateService accessGateService
    ) {
        this.contractMatcher = contractMatcher;
        this.accessGateService = accessGateService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<V21ApiContract> contract = contractMatcher.match(request.getMethod(), request.getRequestURI());
        AccessDecision decision = accessGateService.evaluate(accessRequest(request, contract.orElse(null)));
        if (decision.allowed()) {
            return true;
        }
        throw new BizException(errorCode(decision.errorCode()), decision.message());
    }

    private static AccessRequest accessRequest(HttpServletRequest request, V21ApiContract contract) {
        JwtPrincipal principal = currentPrincipal();
        return new AccessRequest(
                request.getMethod(),
                request.getRequestURI(),
                principal == null ? null : principal.userId(),
                principal == null ? null : principal.role(),
                request.getHeader(TenantScopeHeaders.ORG_ID),
                request.getHeader(TenantScopeHeaders.SCOPE_ID),
                contract
        );
    }

    private static JwtPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private static ErrorCode errorCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Unknown error code: " + code);
    }
}
