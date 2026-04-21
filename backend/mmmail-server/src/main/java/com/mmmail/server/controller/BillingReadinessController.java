package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/billing")
public class BillingReadinessController {

    @GetMapping("/readiness")
    public Result<Map<String, Object>> readReadiness() {
        return Result.success(Map.of(
                "panels", new String[]{"plans", "billing", "operations", "boundary"},
                "legacyExitReady", true
        ));
    }
}
