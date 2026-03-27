package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.mapper.VpnConnectionProfileMapper;
import com.mmmail.server.model.dto.UpdateVpnSettingsRequest;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.entity.VpnConnectionProfile;
import com.mmmail.server.model.enums.VpnDefaultConnectionMode;
import com.mmmail.server.model.enums.VpnNetShieldMode;
import com.mmmail.server.model.vo.VpnSettingsVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VpnSettingsService {

    private static final VpnNetShieldMode DEFAULT_NETSHIELD_MODE = VpnNetShieldMode.OFF;
    private static final int DEFAULT_KILL_SWITCH_ENABLED = 0;
    private static final VpnDefaultConnectionMode DEFAULT_CONNECTION_MODE = VpnDefaultConnectionMode.FASTEST;

    private final UserPreferenceMapper userPreferenceMapper;
    private final VpnConnectionProfileMapper vpnConnectionProfileMapper;
    private final AuditService auditService;

    public VpnSettingsService(
            UserPreferenceMapper userPreferenceMapper,
            VpnConnectionProfileMapper vpnConnectionProfileMapper,
            AuditService auditService
    ) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.vpnConnectionProfileMapper = vpnConnectionProfileMapper;
        this.auditService = auditService;
    }

    public VpnSettingsVo getSettings(Long userId, String ipAddress) {
        ResolvedVpnSettings settings = resolveSettings(userId);
        auditService.record(userId, "VPN_SETTINGS_GET", "defaultMode=" + settings.defaultConnectionMode(), ipAddress);
        return toVo(settings);
    }

    public ResolvedVpnSettings resolveSettings(Long userId) {
        return resolveSettings(loadPreference(userId));
    }

    @Transactional
    public VpnSettingsVo updateSettings(Long userId, UpdateVpnSettingsRequest request, String ipAddress) {
        UserPreference preference = loadPreference(userId);
        ResolvedVpnSettings current = resolveSettings(preference);
        ResolvedVpnSettings next = mergeSettings(current, request);
        ensureDefaultProfileOwned(userId, next);
        persistPreference(userId, preference, next);
        auditService.record(
                userId,
                "VPN_SETTINGS_UPDATE",
                "defaultMode=" + next.defaultConnectionMode() + ",killSwitch=" + next.killSwitchEnabled(),
                ipAddress
        );
        return toVo(next);
    }

    @Transactional
    public void clearDefaultProfileIfMatches(Long userId, Long profileId) {
        UserPreference preference = loadPreference(userId);
        if (preference.getId() == null || profileId == null || !profileId.equals(preference.getVpnDefaultProfileId())) {
            return;
        }
        ResolvedVpnSettings current = resolveSettings(preference);
        updateVpnColumns(
                preference.getId(),
                userId,
                new ResolvedVpnSettings(
                        current.netshieldMode(),
                        current.killSwitchEnabled(),
                        DEFAULT_CONNECTION_MODE,
                        null
                ),
                LocalDateTime.now()
        );
    }

    private ResolvedVpnSettings mergeSettings(ResolvedVpnSettings current, UpdateVpnSettingsRequest request) {
        VpnNetShieldMode netshieldMode = VpnNetShieldMode.resolve(request.netshieldMode(), current.netshieldMode());
        boolean killSwitchEnabled = request.killSwitchEnabled() == null ? current.killSwitchEnabled() : request.killSwitchEnabled();
        VpnDefaultConnectionMode defaultMode = VpnDefaultConnectionMode.resolve(request.defaultConnectionMode(), current.defaultConnectionMode());
        Long requestedProfileId = VpnProfileIdSupport.parseNullable(request.defaultProfileId(), "VPN default profile is invalid");
        Long defaultProfileId = request.defaultProfileId() == null ? current.defaultProfileId() : requestedProfileId;
        if (defaultMode == VpnDefaultConnectionMode.PROFILE && defaultProfileId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN default profile is required");
        }
        return new ResolvedVpnSettings(netshieldMode, killSwitchEnabled, defaultMode, defaultProfileId);
    }

    private void ensureDefaultProfileOwned(Long userId, ResolvedVpnSettings settings) {
        if (settings.defaultConnectionMode() != VpnDefaultConnectionMode.PROFILE || settings.defaultProfileId() == null) {
            return;
        }
        VpnConnectionProfile profile = vpnConnectionProfileMapper.selectOne(new LambdaQueryWrapper<VpnConnectionProfile>()
                .eq(VpnConnectionProfile::getId, settings.defaultProfileId())
                .eq(VpnConnectionProfile::getOwnerId, userId));
        if (profile == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "VPN default profile is not found");
        }
    }

    private UserPreference loadPreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        if (preference != null) {
            applyDefaults(preference);
            return preference;
        }
        UserPreference created = new UserPreference();
        created.setOwnerId(userId);
        created.setSignature("");
        created.setTimezone("UTC");
        created.setPreferredLocale("en");
        created.setMailAddressMode("PROTON_ADDRESS");
        created.setAutoSaveSeconds(15);
        created.setUndoSendSeconds(10);
        created.setDriveVersionRetentionCount(50);
        created.setDriveVersionRetentionDays(365);
        created.setAuthenticatorSyncEnabled(1);
        created.setAuthenticatorEncryptedBackupEnabled(0);
        created.setAuthenticatorPinProtectionEnabled(0);
        created.setAuthenticatorLockTimeoutSeconds(300);
        applyDefaults(created);
        return created;
    }

    private void persistPreference(Long userId, UserPreference preference, ResolvedVpnSettings settings) {
        LocalDateTime now = LocalDateTime.now();
        preference.setOwnerId(userId);
        preference.setVpnNetshieldMode(settings.netshieldMode().name());
        preference.setVpnKillSwitchEnabled(settings.killSwitchEnabled() ? 1 : 0);
        preference.setVpnDefaultConnectionMode(settings.defaultConnectionMode().name());
        preference.setVpnDefaultProfileId(settings.defaultProfileId());
        preference.setUpdatedAt(now);
        if (preference.getId() == null) {
            preference.setCreatedAt(now);
            preference.setDeleted(0);
            userPreferenceMapper.insert(preference);
            return;
        }
        updateVpnColumns(preference.getId(), userId, settings, now);
    }

    private void updateVpnColumns(Long preferenceId, Long userId, ResolvedVpnSettings settings, LocalDateTime updatedAt) {
        LambdaUpdateWrapper<UserPreference> update = new LambdaUpdateWrapper<UserPreference>()
                .eq(UserPreference::getId, preferenceId)
                .eq(UserPreference::getOwnerId, userId)
                .set(UserPreference::getVpnNetshieldMode, settings.netshieldMode().name())
                .set(UserPreference::getVpnKillSwitchEnabled, settings.killSwitchEnabled() ? 1 : 0)
                .set(UserPreference::getVpnDefaultConnectionMode, settings.defaultConnectionMode().name())
                .set(UserPreference::getUpdatedAt, updatedAt);
        if (settings.defaultProfileId() == null) {
            update.setSql("vpn_default_profile_id = null");
        } else {
            update.set(UserPreference::getVpnDefaultProfileId, settings.defaultProfileId());
        }
        userPreferenceMapper.update(null, update);
    }

    private void applyDefaults(UserPreference preference) {
        if (preference.getVpnNetshieldMode() == null) {
            preference.setVpnNetshieldMode(DEFAULT_NETSHIELD_MODE.name());
        }
        if (preference.getVpnKillSwitchEnabled() == null) {
            preference.setVpnKillSwitchEnabled(DEFAULT_KILL_SWITCH_ENABLED);
        }
        if (preference.getVpnDefaultConnectionMode() == null) {
            preference.setVpnDefaultConnectionMode(DEFAULT_CONNECTION_MODE.name());
        }
    }

    private ResolvedVpnSettings resolveSettings(UserPreference preference) {
        applyDefaults(preference);
        return new ResolvedVpnSettings(
                VpnNetShieldMode.resolve(preference.getVpnNetshieldMode(), DEFAULT_NETSHIELD_MODE),
                preference.getVpnKillSwitchEnabled() != null && preference.getVpnKillSwitchEnabled() == 1,
                VpnDefaultConnectionMode.resolve(preference.getVpnDefaultConnectionMode(), DEFAULT_CONNECTION_MODE),
                preference.getVpnDefaultProfileId()
        );
    }

    private VpnSettingsVo toVo(ResolvedVpnSettings settings) {
        return new VpnSettingsVo(
                settings.netshieldMode().name(),
                settings.killSwitchEnabled(),
                settings.defaultConnectionMode().name(),
                settings.defaultProfileId() == null ? null : String.valueOf(settings.defaultProfileId())
        );
    }

    public record ResolvedVpnSettings(
            VpnNetShieldMode netshieldMode,
            boolean killSwitchEnabled,
            VpnDefaultConnectionMode defaultConnectionMode,
            Long defaultProfileId
    ) {
    }
}
