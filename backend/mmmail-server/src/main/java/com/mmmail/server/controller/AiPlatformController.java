package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/ai-platform")
public class AiPlatformController {

    @GetMapping("/capabilities")
    public Result<Map<String, Object>> readCapabilities() {
        return Result.success(Map.of(
                "runStates", new String[]{"queued", "running", "waiting-approval", "succeeded", "failed", "retryable"},
                "supportsPreview", true,
                "supportsApproval", true,
                "supportsAudit", true
        ));
    }
}
