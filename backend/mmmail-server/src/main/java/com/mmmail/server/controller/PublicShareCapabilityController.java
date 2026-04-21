package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/public-share/capabilities")
public class PublicShareCapabilityController {

    @GetMapping
    public Result<Map<String, Object>> readCapabilities() {
        return Result.success(Map.of(
                "states", new String[]{"token-valid", "password-required", "unlocked", "expired", "revoked", "locked", "download-blocked"},
                "auditedActions", new String[]{"preview", "download", "copy", "reshare"},
                "passwordHeader", "X-Drive-Share-Password"
        ));
    }
}
