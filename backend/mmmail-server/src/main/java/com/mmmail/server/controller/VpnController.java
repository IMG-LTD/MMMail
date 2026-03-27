package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.ConnectVpnSessionRequest;
import com.mmmail.server.model.dto.CreateVpnProfileRequest;
import com.mmmail.server.model.dto.QuickConnectVpnSessionRequest;
import com.mmmail.server.model.dto.UpdateVpnProfileRequest;
import com.mmmail.server.model.dto.UpdateVpnSettingsRequest;
import com.mmmail.server.model.vo.VpnConnectionProfileVo;
import com.mmmail.server.model.vo.VpnServerVo;
import com.mmmail.server.model.vo.VpnSessionVo;
import com.mmmail.server.model.vo.VpnSettingsVo;
import com.mmmail.server.service.VpnProfileService;
import com.mmmail.server.service.VpnSettingsService;
import com.mmmail.server.service.VpnService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vpn")
public class VpnController {

    private final VpnService vpnService;
    private final VpnSettingsService vpnSettingsService;
    private final VpnProfileService vpnProfileService;

    public VpnController(
            VpnService vpnService,
            VpnSettingsService vpnSettingsService,
            VpnProfileService vpnProfileService
    ) {
        this.vpnService = vpnService;
        this.vpnSettingsService = vpnSettingsService;
        this.vpnProfileService = vpnProfileService;
    }

    @GetMapping("/servers")
    public Result<List<VpnServerVo>> listServers(HttpServletRequest httpRequest) {
        return Result.success(vpnService.listServers(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/settings")
    public Result<VpnSettingsVo> getSettings(HttpServletRequest httpRequest) {
        return Result.success(vpnSettingsService.getSettings(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/settings")
    public Result<VpnSettingsVo> updateSettings(
            @Valid @RequestBody UpdateVpnSettingsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnSettingsService.updateSettings(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/profiles")
    public Result<List<VpnConnectionProfileVo>> listProfiles(HttpServletRequest httpRequest) {
        return Result.success(vpnProfileService.listProfiles(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/profiles")
    public Result<VpnConnectionProfileVo> createProfile(
            @Valid @RequestBody CreateVpnProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnProfileService.createProfile(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/profiles/{profileId}")
    public Result<VpnConnectionProfileVo> updateProfile(
            @PathVariable @NotNull Long profileId,
            @Valid @RequestBody UpdateVpnProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnProfileService.updateProfile(
                SecurityUtils.currentUserId(),
                profileId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/profiles/{profileId}")
    public Result<Boolean> deleteProfile(
            @PathVariable @NotNull Long profileId,
            HttpServletRequest httpRequest
    ) {
        vpnProfileService.deleteProfile(SecurityUtils.currentUserId(), profileId, httpRequest.getRemoteAddr());
        return Result.success(Boolean.TRUE);
    }

    @GetMapping("/sessions/current")
    public Result<VpnSessionVo> current(HttpServletRequest httpRequest) {
        return Result.success(vpnService.current(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/sessions/history")
    public Result<List<VpnSessionVo>> history(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnService.history(SecurityUtils.currentUserId(), limit, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/sessions/connect")
    public Result<VpnSessionVo> connect(
            @Valid @RequestBody ConnectVpnSessionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnService.connect(
                SecurityUtils.currentUserId(),
                request.serverId(),
                request.protocol(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/sessions/quick-connect")
    public Result<VpnSessionVo> quickConnect(
            @RequestBody(required = false) QuickConnectVpnSessionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(vpnService.quickConnect(
                SecurityUtils.currentUserId(),
                request == null ? null : request.profileId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/sessions/disconnect")
    public Result<VpnSessionVo> disconnect(HttpServletRequest httpRequest) {
        return Result.success(vpnService.disconnect(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }
}
