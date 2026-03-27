package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.service.AuditService;
import com.mmmail.server.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    public Result<List<AuditEventVo>> events() {
        Long userId = SecurityUtils.currentUserId();
        boolean admin = SecurityUtils.isAdmin();
        return Result.success(auditService.list(userId, admin));
    }
}
