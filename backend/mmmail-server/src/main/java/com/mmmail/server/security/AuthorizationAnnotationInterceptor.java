package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.service.OrgProductAccessGuardService;
import com.mmmail.server.service.OrgProductAccessService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@Component
public class AuthorizationAnnotationInterceptor implements HandlerInterceptor {

    private static final String ROLE_DENIED = "Required role is not granted";

    private final OrgProductAccessGuardService orgProductAccessGuardService;
    private final OrgProductAccessService orgProductAccessService;

    public AuthorizationAnnotationInterceptor(
            OrgProductAccessGuardService orgProductAccessGuardService,
            OrgProductAccessService orgProductAccessService
    ) {
        this.orgProductAccessGuardService = orgProductAccessGuardService;
        this.orgProductAccessService = orgProductAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        enforceRole(findAnnotation(handlerMethod, RequireRole.class));
        enforceEntitlement(findAnnotation(handlerMethod, RequireEntitlement.class), request);
        return true;
    }

    private void enforceRole(RequireRole gate) {
        if (gate == null) {
            return;
        }
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        boolean granted = Arrays.stream(gate.value()).anyMatch(role -> role.equalsIgnoreCase(principal.role()));
        if (!granted) {
            throw new BizException(ErrorCode.V2_PERMISSION_DENIED, ROLE_DENIED);
        }
    }

    private void enforceEntitlement(RequireEntitlement gate, HttpServletRequest request) {
        if (gate == null) {
            return;
        }
        JwtPrincipal principal = SecurityUtils.currentPrincipal();
        Long orgId = orgProductAccessGuardService.resolveActiveOrgId(request);
        if (orgId == null) {
            return;
        }
        for (String entitlement : gate.value()) {
            orgProductAccessService.assertCurrentUserProductEnabled(principal.userId(), orgId, entitlement);
        }
    }

    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> type) {
        A methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), type);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), type);
    }
}
