package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/mcp")
public class McpRegistryController {

    @GetMapping("/registry")
    public Result<Map<String, Object>> readRegistry() {
        return Result.success(Map.of(
                "supportsGrantMatrix", true,
                "supportsHealthChecks", true,
                "supportsSecretMasking", true,
                "supportsAudit", true
        ));
    }
}
