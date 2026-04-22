package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.foundation.tenant.TenantScopeHeaders;
import com.mmmail.platform.audit.AuditEventType;
import com.mmmail.platform.jobs.JobRunState;
import com.mmmail.platform.outbox.OutboxEventStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/platform/capabilities")
public class PlatformCapabilityController {

    @GetMapping
    public Result<Map<String, Object>> readCapabilities() {
        return Result.success(Map.of(
                "jobRunStates", Arrays.stream(JobRunState.values()).map(JobRunState::name).toArray(String[]::new),
                "outboxStatuses", Arrays.stream(OutboxEventStatus.values()).map(OutboxEventStatus::name).toArray(String[]::new),
                "auditEventTypes", Arrays.stream(AuditEventType.values()).map(AuditEventType::name).toArray(String[]::new),
                "softAuthSupported", true,
                "scopeHeaders", new String[]{TenantScopeHeaders.ORG_ID, TenantScopeHeaders.SCOPE_ID}
        ));
    }
}
