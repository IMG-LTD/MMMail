package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.UpdateAuthenticatorSecurityRequest;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.AuthenticatorSecurityPinVerificationVo;
import com.mmmail.server.model.vo.AuthenticatorSecurityPreferenceVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthenticatorSecurityPreferenceService {

    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 300;

    private final UserPreferenceMapper userPreferenceMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthenticatorSecurityPreferenceService(
            UserPreferenceMapper userPreferenceMapper,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public AuthenticatorSecurityPreferenceVo get(Long userId) {
        return toVo(findOrCreatePreference(userId));
    }

    @Transactional
    public AuthenticatorSecurityPreferenceVo update(Long userId, UpdateAuthenticatorSecurityRequest request, String ipAddress) {
        UserPreference preference = findOrCreatePreference(userId);
        LocalDateTime now = LocalDateTime.now();
        preference.setAuthenticatorSyncEnabled(toFlag(request.syncEnabled()));
        preference.setAuthenticatorEncryptedBackupEnabled(toFlag(request.encryptedBackupEnabled()));
        preference.setAuthenticatorPinProtectionEnabled(toFlag(request.pinProtectionEnabled()));
        preference.setAuthenticatorLockTimeoutSeconds(request.lockTimeoutSeconds());
        applyPin(preference, request.pinProtectionEnabled(), request.pin());
        if (request.syncEnabled() && preference.getAuthenticatorLastSyncedAt() == null) {
            preference.setAuthenticatorLastSyncedAt(now);
        }
        preference.setUpdatedAt(now);
        persist(preference, now);
        auditService.record(
                userId,
                "AUTH_SECURITY_UPDATE",
                "sync=" + request.syncEnabled() + ",backup=" + request.encryptedBackupEnabled() + ",pin=" + request.pinProtectionEnabled(),
                ipAddress
        );
        return toVo(preference);
    }

    public AuthenticatorSecurityPinVerificationVo verifyPin(Long userId, String pin, String ipAddress) {
        UserPreference preference = findOrCreatePreference(userId);
        if (!isEnabled(preference.getAuthenticatorPinProtectionEnabled()) || !StringUtils.hasText(preference.getAuthenticatorPinHash())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator PIN is not configured");
        }
        if (!passwordEncoder.matches(pin, preference.getAuthenticatorPinHash())) {
            auditService.record(userId, "AUTH_PIN_VERIFY_FAILED", "pin verification failed", ipAddress);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator PIN is invalid");
        }
        auditService.record(userId, "AUTH_PIN_VERIFY", "pin verification succeeded", ipAddress);
        return new AuthenticatorSecurityPinVerificationVo(true, resolveLockTimeout(preference));
    }

    @Transactional
    public void markSynced(Long userId, LocalDateTime syncedAt) {
        UserPreference preference = findOrCreatePreference(userId);
        if (!isEnabled(preference.getAuthenticatorSyncEnabled())) {
            return;
        }
        preference.setAuthenticatorLastSyncedAt(syncedAt);
        preference.setUpdatedAt(syncedAt);
        persist(preference, syncedAt);
    }

    @Transactional
    public void markBackedUp(Long userId, LocalDateTime backedUpAt) {
        UserPreference preference = findOrCreatePreference(userId);
        preference.setAuthenticatorLastBackupAt(backedUpAt);
        preference.setUpdatedAt(backedUpAt);
        persist(preference, backedUpAt);
    }

    private void applyPin(UserPreference preference, boolean pinEnabled, String pin) {
        if (!pinEnabled) {
            preference.setAuthenticatorPinHash(null);
            return;
        }
        if (StringUtils.hasText(pin)) {
            preference.setAuthenticatorPinHash(passwordEncoder.encode(pin));
            return;
        }
        if (!StringUtils.hasText(preference.getAuthenticatorPinHash())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator PIN is required when PIN protection is enabled");
        }
    }

    private UserPreference findOrCreatePreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId)
                .last("limit 1"));
        if (preference != null) {
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
        created.setAuthenticatorLockTimeoutSeconds(DEFAULT_LOCK_TIMEOUT_SECONDS);
        return created;
    }

    private void persist(UserPreference preference, LocalDateTime now) {
        if (preference.getId() == null) {
            preference.setCreatedAt(now);
            preference.setDeleted(0);
            userPreferenceMapper.insert(preference);
            return;
        }
        userPreferenceMapper.updateById(preference);
    }

    private AuthenticatorSecurityPreferenceVo toVo(UserPreference preference) {
        return new AuthenticatorSecurityPreferenceVo(
                isEnabled(preference.getAuthenticatorSyncEnabled()),
                isEnabled(preference.getAuthenticatorEncryptedBackupEnabled()),
                isEnabled(preference.getAuthenticatorPinProtectionEnabled()),
                StringUtils.hasText(preference.getAuthenticatorPinHash()),
                resolveLockTimeout(preference),
                preference.getAuthenticatorLastSyncedAt(),
                preference.getAuthenticatorLastBackupAt()
        );
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private int toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private int resolveLockTimeout(UserPreference preference) {
        Integer timeout = preference.getAuthenticatorLockTimeoutSeconds();
        return timeout == null ? DEFAULT_LOCK_TIMEOUT_SECONDS : timeout;
    }
}
