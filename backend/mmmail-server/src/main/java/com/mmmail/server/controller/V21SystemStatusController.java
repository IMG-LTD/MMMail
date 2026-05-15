package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.PublicSystemStatusVo;
import com.mmmail.server.model.vo.SystemHealthOverviewVo;
import com.mmmail.server.observability.SystemHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/system")
public class V21SystemStatusController {

    private static final String DEGRADED = "degraded";
    private static final String OFFLINE = "offline";
    private static final String OPERATIONAL = "operational";

    private final SystemHealthService systemHealthService;

    public V21SystemStatusController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping({"/status", "/health"})
    public Result<PublicSystemStatusVo> status() {
        SystemHealthOverviewVo overview = systemHealthService.getOverview();
        return Result.success(new PublicSystemStatusVo(null, null, publicStatus(overview.status()), overview.generatedAt().toString()));
    }

    private static String publicStatus(String status) {
        if ("UP".equalsIgnoreCase(status)) {
            return OPERATIONAL;
        }
        if ("DEGRADED".equalsIgnoreCase(status)) {
            return DEGRADED;
        }
        return OFFLINE;
    }
}
