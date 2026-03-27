package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.UpdateProfileRequest;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.enums.MailAddressMode;
import com.mmmail.server.model.vo.UserPreferenceVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserPreferenceService {

    private static final int DEFAULT_DRIVE_VERSION_RETENTION_COUNT = 50;
    private static final int DEFAULT_DRIVE_VERSION_RETENTION_DAYS = 365;
    private static final String DEFAULT_PREFERRED_LOCALE = "en";
    private static final int DEFAULT_AUTHENTICATOR_SYNC_ENABLED = 1;
    private static final int DEFAULT_AUTHENTICATOR_BACKUP_ENABLED = 0;
    private static final int DEFAULT_AUTHENTICATOR_PIN_PROTECTION_ENABLED = 0;
    private static final int DEFAULT_AUTHENTICATOR_LOCK_TIMEOUT_SECONDS = 300;

    private final UserAccountMapper userAccountMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditService auditService;

    public UserPreferenceService(
            UserAccountMapper userAccountMapper,
            UserPreferenceMapper userPreferenceMapper,
            AuditService auditService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditService = auditService;
    }

    public UserPreferenceVo getProfile(Long userId) {
        UserAccount user = requireUser(userId);
        UserPreference preference = findOrCreatePreference(userId);
        return toVo(user, preference);
    }

    public String resolveMailAddressMode(Long userId) {
        return MailAddressMode.resolveStoredValue(findOrCreatePreference(userId).getMailAddressMode(), null);
    }

    public boolean isExternalAccount(Long userId) {
        return MailAddressMode.isExternalAccount(resolveMailAddressMode(userId));
    }

    @Transactional
    public UserPreferenceVo updateProfile(Long userId, UpdateProfileRequest request, String ipAddress) {
        UserAccount user = requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        user.setDisplayName(request.displayName());
        user.setUpdatedAt(now);
        userAccountMapper.updateById(user);

        UserPreference preference = findOrCreatePreference(userId);
        preference.setSignature(request.signature());
        preference.setTimezone(request.timezone());
        preference.setPreferredLocale(resolvePreferredLocale(
                request.preferredLocale(),
                preference.getPreferredLocale()
        ));
        preference.setMailAddressMode(MailAddressMode.resolveStoredValue(
                request.mailAddressMode(),
                preference.getMailAddressMode()
        ));
        preference.setAutoSaveSeconds(request.autoSaveSeconds());
        preference.setUndoSendSeconds(request.undoSendSeconds());
        preference.setDriveVersionRetentionCount(resolveRetentionCount(
                request.driveVersionRetentionCount(),
                preference.getDriveVersionRetentionCount()
        ));
        preference.setDriveVersionRetentionDays(resolveRetentionDays(
                request.driveVersionRetentionDays(),
                preference.getDriveVersionRetentionDays()
        ));
        applyAuthenticatorDefaults(preference);
        preference.setUpdatedAt(now);

        if (preference.getId() == null) {
            preference.setCreatedAt(now);
            preference.setDeleted(0);
            userPreferenceMapper.insert(preference);
        } else {
            userPreferenceMapper.updateById(preference);
        }

        auditService.record(userId, "PROFILE_UPDATED", "User preferences updated", ipAddress);
        return toVo(user, preference);
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserPreference findOrCreatePreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        if (preference != null) {
            return preference;
        }

        UserPreference defaultPreference = new UserPreference();
        defaultPreference.setOwnerId(userId);
        defaultPreference.setSignature("");
        defaultPreference.setTimezone("UTC");
        defaultPreference.setPreferredLocale(DEFAULT_PREFERRED_LOCALE);
        defaultPreference.setMailAddressMode(MailAddressMode.PROTON_ADDRESS.name());
        defaultPreference.setAutoSaveSeconds(15);
        defaultPreference.setUndoSendSeconds(10);
        defaultPreference.setDriveVersionRetentionCount(DEFAULT_DRIVE_VERSION_RETENTION_COUNT);
        defaultPreference.setDriveVersionRetentionDays(DEFAULT_DRIVE_VERSION_RETENTION_DAYS);
        defaultPreference.setAuthenticatorSyncEnabled(DEFAULT_AUTHENTICATOR_SYNC_ENABLED);
        defaultPreference.setAuthenticatorEncryptedBackupEnabled(DEFAULT_AUTHENTICATOR_BACKUP_ENABLED);
        defaultPreference.setAuthenticatorPinProtectionEnabled(DEFAULT_AUTHENTICATOR_PIN_PROTECTION_ENABLED);
        defaultPreference.setAuthenticatorLockTimeoutSeconds(DEFAULT_AUTHENTICATOR_LOCK_TIMEOUT_SECONDS);
        return defaultPreference;
    }

    private UserPreferenceVo toVo(UserAccount user, UserPreference preference) {
        return new UserPreferenceVo(
                user.getDisplayName(),
                preference.getSignature() == null ? "" : preference.getSignature(),
                preference.getTimezone() == null ? "UTC" : preference.getTimezone(),
                resolvePreferredLocale(preference.getPreferredLocale(), null),
                MailAddressMode.resolveStoredValue(preference.getMailAddressMode(), null),
                preference.getAutoSaveSeconds() == null ? 15 : preference.getAutoSaveSeconds(),
                preference.getUndoSendSeconds() == null ? 10 : preference.getUndoSendSeconds(),
                resolveRetentionCount(preference.getDriveVersionRetentionCount(), null),
                resolveRetentionDays(preference.getDriveVersionRetentionDays(), null)
        );
    }

    private String resolvePreferredLocale(String candidate, String fallback) {
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return DEFAULT_PREFERRED_LOCALE;
    }

    private int resolveRetentionCount(Integer candidate, Integer fallback) {
        if (candidate != null) {
            return candidate;
        }
        if (fallback != null) {
            return fallback;
        }
        return DEFAULT_DRIVE_VERSION_RETENTION_COUNT;
    }

    private int resolveRetentionDays(Integer candidate, Integer fallback) {
        if (candidate != null) {
            return candidate;
        }
        if (fallback != null) {
            return fallback;
        }
        return DEFAULT_DRIVE_VERSION_RETENTION_DAYS;
    }

    private void applyAuthenticatorDefaults(UserPreference preference) {
        if (preference.getAuthenticatorSyncEnabled() == null) {
            preference.setAuthenticatorSyncEnabled(DEFAULT_AUTHENTICATOR_SYNC_ENABLED);
        }
        if (preference.getAuthenticatorEncryptedBackupEnabled() == null) {
            preference.setAuthenticatorEncryptedBackupEnabled(DEFAULT_AUTHENTICATOR_BACKUP_ENABLED);
        }
        if (preference.getAuthenticatorPinProtectionEnabled() == null) {
            preference.setAuthenticatorPinProtectionEnabled(DEFAULT_AUTHENTICATOR_PIN_PROTECTION_ENABLED);
        }
        if (preference.getAuthenticatorLockTimeoutSeconds() == null) {
            preference.setAuthenticatorLockTimeoutSeconds(DEFAULT_AUTHENTICATOR_LOCK_TIMEOUT_SECONDS);
        }
    }
}
