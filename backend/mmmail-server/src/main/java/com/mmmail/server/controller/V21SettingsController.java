package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.PatchV21NotificationSettingsRequest;
import com.mmmail.server.model.dto.PatchV21SecuritySettingsRequest;
import com.mmmail.server.model.dto.UpdateProfileRequest;
import com.mmmail.server.model.vo.UserPreferenceVo;
import com.mmmail.server.model.vo.V21DeviceSessionVo;
import com.mmmail.server.model.vo.V21NotificationSettingsVo;
import com.mmmail.server.model.vo.V21SecuritySettingsVo;
import com.mmmail.server.service.V21SettingsRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v2/settings")
public class V21SettingsController {

    private final V21SettingsRuntimeBridgeService settingsRuntimeBridgeService;

    public V21SettingsController(V21SettingsRuntimeBridgeService settingsRuntimeBridgeService) {
        this.settingsRuntimeBridgeService = settingsRuntimeBridgeService;
    }

    @GetMapping("/profile")
    public Result<UserPreferenceVo> profile() {
        return Result.success(settingsRuntimeBridgeService.profile(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/profile")
    public Result<UserPreferenceVo> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(settingsRuntimeBridgeService.updateProfile(
                SecurityUtils.currentUserId(),
                request,
                httpRequest
        ));
    }

    @GetMapping("/security")
    public Result<V21SecuritySettingsVo> security() {
        return Result.success(settingsRuntimeBridgeService.security());
    }

    @PatchMapping("/security")
    public Result<Void> patchSecurity(@Valid @RequestBody PatchV21SecuritySettingsRequest request) {
        settingsRuntimeBridgeService.patchSecurity(request);
        return Result.success(null);
    }

    @GetMapping("/devices")
    public Result<List<V21DeviceSessionVo>> devices() {
        return Result.success(settingsRuntimeBridgeService.devices(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId()
        ));
    }

    @DeleteMapping("/devices/{id}")
    public Result<Void> deleteDevice(@PathVariable String id, HttpServletRequest request) {
        settingsRuntimeBridgeService.deleteDevice(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                id,
                request
        );
        return Result.success(null);
    }

    @GetMapping("/notifications")
    public Result<V21NotificationSettingsVo> notifications() {
        return Result.success(settingsRuntimeBridgeService.notifications());
    }

    @PatchMapping("/notifications")
    public Result<Void> patchNotifications(@Valid @RequestBody PatchV21NotificationSettingsRequest request) {
        settingsRuntimeBridgeService.patchNotifications(request);
        return Result.success(null);
    }
}
