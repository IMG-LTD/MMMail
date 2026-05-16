package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.SecurityEventActionRequest;
import com.mmmail.server.model.vo.SecurityEventVo;
import com.mmmail.server.security.RequireRole;
import com.mmmail.server.service.SecurityEventService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SecurityEventController {

    private final SecurityEventService securityEventService;

    public SecurityEventController(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @GetMapping("/api/v1/security/events")
    public Result<List<SecurityEventVo>> events(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page
    ) {
        return Result.success(securityEventService.listUserEvents(SecurityUtils.currentUserId(), type, page));
    }

    @PostMapping("/api/v1/security/events/{id}/ack")
    public Result<SecurityEventVo> acknowledge(@PathVariable Long id) {
        return Result.success(securityEventService.acknowledge(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/api/v1/admin/security/anomalies")
    @RequireRole("ADMIN")
    public Result<List<SecurityEventVo>> anomalies() {
        return Result.success(securityEventService.listAdminAnomalies());
    }

    @PostMapping("/api/v1/admin/security/anomalies/{id}/action")
    @RequireRole("ADMIN")
    public Result<SecurityEventVo> action(
            @PathVariable Long id,
            @Valid @RequestBody SecurityEventActionRequest request
    ) {
        return Result.success(securityEventService.applyAdminAction(
                SecurityUtils.currentUserId(),
                id,
                request.action()
        ));
    }
}
