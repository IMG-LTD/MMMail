package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateClientErrorEventRequest;
import com.mmmail.server.model.vo.SystemHealthOverviewVo;
import com.mmmail.server.observability.ErrorTrackingService;
import com.mmmail.server.observability.SystemHealthService;
import com.mmmail.server.security.SecurityRateLimitService;
import com.mmmail.server.service.AuditService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;
    private final ErrorTrackingService errorTrackingService;
    private final SecurityRateLimitService securityRateLimitService;
    private final AuditService auditService;

    public SystemHealthController(
            SystemHealthService systemHealthService,
            ErrorTrackingService errorTrackingService,
            SecurityRateLimitService securityRateLimitService,
            AuditService auditService
    ) {
        this.systemHealthService = systemHealthService;
        this.errorTrackingService = errorTrackingService;
        this.securityRateLimitService = securityRateLimitService;
        this.auditService = auditService;
    }

    @GetMapping("/health")
    public Result<SystemHealthOverviewVo> getHealth(HttpServletRequest request) {
        requireAdmin();
        SystemHealthOverviewVo overview = systemHealthService.getOverview();
        auditService.record(SecurityUtils.currentUserId(), "SYSTEM_HEALTH_VIEW", "status=" + overview.status(), request.getRemoteAddr());
        return Result.success(overview);
    }

    @PostMapping("/errors/client")
    public Result<Void> reportClientError(
            @Valid @RequestBody CreateClientErrorEventRequest request,
            HttpServletRequest httpRequest
    ) {
        securityRateLimitService.recordClientErrorEvent(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                httpRequest.getRemoteAddr()
        );
        errorTrackingService.recordClientError(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                resolveOrgId(httpRequest),
                request,
                httpRequest.getHeader("User-Agent")
        );
        return Result.success(null);
    }

    private void requireAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "System health requires admin role");
        }
    }

    private String resolveOrgId(HttpServletRequest request) {
        String orgId = request.getHeader("X-MMMAIL-ORG-ID");
        return StringUtils.hasText(orgId) ? orgId.trim() : null;
    }
}
