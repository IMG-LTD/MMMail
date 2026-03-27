package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.VpnConnectionProfileMapper;
import com.mmmail.server.model.dto.CreateVpnProfileRequest;
import com.mmmail.server.model.dto.UpdateVpnProfileRequest;
import com.mmmail.server.model.entity.VpnConnectionProfile;
import com.mmmail.server.model.enums.VpnNetShieldMode;
import com.mmmail.server.model.enums.VpnProfileRoutingMode;
import com.mmmail.server.model.vo.VpnConnectionProfileVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class VpnProfileService {

    private final VpnConnectionProfileMapper vpnConnectionProfileMapper;
    private final VpnServerCatalogService vpnServerCatalogService;
    private final VpnSettingsService vpnSettingsService;
    private final AuditService auditService;

    public VpnProfileService(
            VpnConnectionProfileMapper vpnConnectionProfileMapper,
            VpnServerCatalogService vpnServerCatalogService,
            VpnSettingsService vpnSettingsService,
            AuditService auditService
    ) {
        this.vpnConnectionProfileMapper = vpnConnectionProfileMapper;
        this.vpnServerCatalogService = vpnServerCatalogService;
        this.vpnSettingsService = vpnSettingsService;
        this.auditService = auditService;
    }

    public List<VpnConnectionProfileVo> listProfiles(Long userId, String ipAddress) {
        List<VpnConnectionProfileVo> profiles = vpnConnectionProfileMapper.selectList(new LambdaQueryWrapper<VpnConnectionProfile>()
                        .eq(VpnConnectionProfile::getOwnerId, userId)
                        .orderByDesc(VpnConnectionProfile::getUpdatedAt))
                .stream()
                .map(this::toVo)
                .toList();
        auditService.record(userId, "VPN_PROFILE_LIST", "count=" + profiles.size(), ipAddress);
        return profiles;
    }

    @Transactional
    public VpnConnectionProfileVo createProfile(Long userId, CreateVpnProfileRequest request, String ipAddress) {
        assertUniqueName(userId, request.name(), null);
        VpnConnectionProfile profile = new VpnConnectionProfile();
        profile.setOwnerId(userId);
        applyRequest(profile, request.name(), request.protocol(), request.routingMode(), request.targetServerId(),
                request.targetCountry(), request.secureCoreEnabled(), request.netshieldMode(), request.killSwitchEnabled());
        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setDeleted(0);
        vpnConnectionProfileMapper.insert(profile);
        auditService.record(userId, "VPN_PROFILE_CREATE", "profileId=" + profile.getId(), ipAddress);
        return toVo(profile);
    }

    @Transactional
    public VpnConnectionProfileVo updateProfile(Long userId, Long profileId, UpdateVpnProfileRequest request, String ipAddress) {
        VpnConnectionProfile profile = requireProfile(userId, profileId);
        assertUniqueName(userId, request.name(), profileId);
        applyRequest(profile, request.name(), request.protocol(), request.routingMode(), request.targetServerId(),
                request.targetCountry(), request.secureCoreEnabled(), request.netshieldMode(), request.killSwitchEnabled());
        profile.setUpdatedAt(LocalDateTime.now());
        vpnConnectionProfileMapper.updateById(profile);
        auditService.record(userId, "VPN_PROFILE_UPDATE", "profileId=" + profileId, ipAddress);
        return toVo(profile);
    }

    @Transactional
    public void deleteProfile(Long userId, Long profileId, String ipAddress) {
        VpnConnectionProfile profile = requireProfile(userId, profileId);
        vpnConnectionProfileMapper.deleteById(profile.getId());
        vpnSettingsService.clearDefaultProfileIfMatches(userId, profile.getId());
        auditService.record(userId, "VPN_PROFILE_DELETE", "profileId=" + profileId, ipAddress);
    }

    public VpnConnectionProfile requireProfile(Long userId, Long profileId) {
        VpnConnectionProfile profile = vpnConnectionProfileMapper.selectOne(new LambdaQueryWrapper<VpnConnectionProfile>()
                .eq(VpnConnectionProfile::getId, profileId)
                .eq(VpnConnectionProfile::getOwnerId, userId)
                .last("limit 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN profile is not found");
        }
        return profile;
    }

    private void assertUniqueName(Long userId, String name, Long profileId) {
        LambdaQueryWrapper<VpnConnectionProfile> query = new LambdaQueryWrapper<VpnConnectionProfile>()
                .eq(VpnConnectionProfile::getOwnerId, userId)
                .eq(VpnConnectionProfile::getName, normalizeName(name));
        if (profileId != null) {
            query.ne(VpnConnectionProfile::getId, profileId);
        }
        if (vpnConnectionProfileMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN profile name already exists");
        }
    }

    private void applyRequest(
            VpnConnectionProfile profile,
            String name,
            String protocol,
            String routingMode,
            String targetServerId,
            String targetCountry,
            Boolean secureCoreEnabled,
            String netshieldMode,
            Boolean killSwitchEnabled
    ) {
        VpnProfileRoutingMode resolvedRoutingMode = VpnProfileRoutingMode.resolve(routingMode, VpnProfileRoutingMode.FASTEST);
        String normalizedCountry = normalizeCountry(targetCountry);
        String normalizedServerId = normalizeServerId(targetServerId);
        validateTargeting(resolvedRoutingMode, normalizedServerId, normalizedCountry);
        profile.setName(normalizeName(name));
        profile.setProtocol(VpnProtocolSupport.normalizeProtocol(protocol));
        profile.setRoutingMode(resolvedRoutingMode.name());
        profile.setTargetServerId(normalizedServerId);
        profile.setTargetCountry(normalizedCountry);
        profile.setSecureCoreEnabled(Boolean.TRUE.equals(secureCoreEnabled) ? 1 : 0);
        profile.setNetshieldMode(VpnNetShieldMode.resolve(netshieldMode, VpnNetShieldMode.OFF).name());
        profile.setKillSwitchEnabled(Boolean.TRUE.equals(killSwitchEnabled) ? 1 : 0);
    }

    private void validateTargeting(VpnProfileRoutingMode routingMode, String targetServerId, String targetCountry) {
        if (routingMode == VpnProfileRoutingMode.SERVER) {
            vpnServerCatalogService.requireServer(targetServerId);
            return;
        }
        if (routingMode == VpnProfileRoutingMode.COUNTRY && !vpnServerCatalogService.isCountryDefined(targetCountry)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN country is unavailable");
        }
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN profile name is required");
        }
        return name.trim();
    }

    private String normalizeServerId(String targetServerId) {
        if (!StringUtils.hasText(targetServerId)) {
            return null;
        }
        return targetServerId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCountry(String targetCountry) {
        if (!StringUtils.hasText(targetCountry)) {
            return null;
        }
        return targetCountry.trim();
    }

    private VpnConnectionProfileVo toVo(VpnConnectionProfile profile) {
        return new VpnConnectionProfileVo(
                String.valueOf(profile.getId()),
                profile.getName(),
                profile.getProtocol(),
                profile.getRoutingMode(),
                profile.getTargetServerId(),
                profile.getTargetCountry(),
                profile.getSecureCoreEnabled() != null && profile.getSecureCoreEnabled() == 1,
                VpnNetShieldMode.resolve(profile.getNetshieldMode(), VpnNetShieldMode.OFF).name(),
                profile.getKillSwitchEnabled() != null && profile.getKillSwitchEnabled() == 1,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
