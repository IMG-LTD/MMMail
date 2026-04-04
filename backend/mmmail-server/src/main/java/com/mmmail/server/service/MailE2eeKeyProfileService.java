package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.UpdateMailE2eeKeyProfileRequest;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.MailE2eeKeyProfileVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MailE2eeKeyProfileService {

    private static final int DISABLED_FLAG = 0;
    private static final int ENABLED_FLAG = 1;
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en";
    private static final String DEFAULT_MAIL_ADDRESS_MODE = "PROTON_ADDRESS";
    private static final int DEFAULT_AUTO_SAVE_SECONDS = 15;
    private static final int DEFAULT_UNDO_SEND_SECONDS = 10;
    private static final int DEFAULT_DRIVE_VERSION_RETENTION_COUNT = 50;
    private static final int DEFAULT_DRIVE_VERSION_RETENTION_DAYS = 365;
    private static final int DEFAULT_AUTHENTICATOR_SYNC_ENABLED = 1;
    private static final int DEFAULT_AUTHENTICATOR_BACKUP_ENABLED = 0;
    private static final int DEFAULT_AUTHENTICATOR_PIN_PROTECTION_ENABLED = 0;
    private static final int DEFAULT_AUTHENTICATOR_LOCK_TIMEOUT_SECONDS = 300;
    private static final String DEFAULT_VPN_NETSHIELD_MODE = "OFF";
    private static final int DEFAULT_VPN_KILL_SWITCH_ENABLED = 0;
    private static final String DEFAULT_VPN_DEFAULT_CONNECTION_MODE = "FASTEST";

    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditService auditService;

    public MailE2eeKeyProfileService(
            UserPreferenceMapper userPreferenceMapper,
            AuditService auditService
    ) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditService = auditService;
    }

    public MailE2eeKeyProfileVo get(Long userId, String ipAddress) {
        MailE2eeKeyProfile profile = resolve(loadPreference(userId));
        auditService.record(userId, "MAIL_E2EE_PROFILE_GET", "enabled=" + profile.enabled(), ipAddress);
        return toVo(profile);
    }

    @Transactional
    public MailE2eeKeyProfileVo update(Long userId, UpdateMailE2eeKeyProfileRequest request, String ipAddress) {
        UserPreference preference = loadPreference(userId);
        MailE2eeKeyProfile current = resolve(preference);
        MailE2eeKeyProfile next = merge(request, current);
        persist(userId, preference, current, next);
        auditService.record(
                userId,
                "MAIL_E2EE_PROFILE_UPDATE",
                "enabled=" + next.enabled() + ",fingerprint=" + safeAuditFingerprint(next.fingerprint()),
                ipAddress
        );
        return toVo(next);
    }

    private UserPreference loadPreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId)
                .last("limit 1"));
        if (preference != null) {
            applyDefaults(preference);
            return preference;
        }
        UserPreference created = new UserPreference();
        applyBaseDefaults(created, userId);
        return created;
    }

    private void applyBaseDefaults(UserPreference preference, Long userId) {
        preference.setOwnerId(userId);
        preference.setSignature("");
        preference.setTimezone(DEFAULT_TIMEZONE);
        preference.setPreferredLocale(DEFAULT_LOCALE);
        preference.setMailAddressMode(DEFAULT_MAIL_ADDRESS_MODE);
        preference.setAutoSaveSeconds(DEFAULT_AUTO_SAVE_SECONDS);
        preference.setUndoSendSeconds(DEFAULT_UNDO_SEND_SECONDS);
        preference.setDriveVersionRetentionCount(DEFAULT_DRIVE_VERSION_RETENTION_COUNT);
        preference.setDriveVersionRetentionDays(DEFAULT_DRIVE_VERSION_RETENTION_DAYS);
        preference.setAuthenticatorSyncEnabled(DEFAULT_AUTHENTICATOR_SYNC_ENABLED);
        preference.setAuthenticatorEncryptedBackupEnabled(DEFAULT_AUTHENTICATOR_BACKUP_ENABLED);
        preference.setAuthenticatorPinProtectionEnabled(DEFAULT_AUTHENTICATOR_PIN_PROTECTION_ENABLED);
        preference.setAuthenticatorLockTimeoutSeconds(DEFAULT_AUTHENTICATOR_LOCK_TIMEOUT_SECONDS);
        preference.setVpnNetshieldMode(DEFAULT_VPN_NETSHIELD_MODE);
        preference.setVpnKillSwitchEnabled(DEFAULT_VPN_KILL_SWITCH_ENABLED);
        preference.setVpnDefaultConnectionMode(DEFAULT_VPN_DEFAULT_CONNECTION_MODE);
        preference.setMailE2eeEnabled(DISABLED_FLAG);
    }

    private void applyDefaults(UserPreference preference) {
        if (preference.getMailE2eeEnabled() == null) {
            preference.setMailE2eeEnabled(DISABLED_FLAG);
        }
    }

    private MailE2eeKeyProfile resolve(UserPreference preference) {
        applyDefaults(preference);
        if (preference.getMailE2eeEnabled() == null || preference.getMailE2eeEnabled() != ENABLED_FLAG) {
            return MailE2eeKeyProfile.disabled();
        }
        return new MailE2eeKeyProfile(
                true,
                preference.getMailE2eeKeyFingerprint(),
                preference.getMailE2eeKeyAlgorithm(),
                preference.getMailE2eePublicKeyArmored(),
                preference.getMailE2eePrivateKeyEncrypted(),
                preference.getMailE2eeKeyCreatedAt()
        );
    }

    private MailE2eeKeyProfile merge(UpdateMailE2eeKeyProfileRequest request, MailE2eeKeyProfile current) {
        if (!Boolean.TRUE.equals(request.enabled())) {
            return MailE2eeKeyProfile.disabled();
        }
        String publicKeyArmored = requireText(request.publicKeyArmored(), "Mail E2EE public key is required");
        String encryptedPrivateKeyArmored = requireText(
                request.encryptedPrivateKeyArmored(),
                "Mail E2EE encrypted private key is required"
        );
        String fingerprint = normalizeFingerprint(request.fingerprint());
        String algorithm = requireText(request.algorithm(), "Mail E2EE algorithm is required");
        LocalDateTime keyCreatedAt = resolveKeyCreatedAt(request.keyCreatedAt(), current.keyCreatedAt());
        return new MailE2eeKeyProfile(
                true,
                fingerprint,
                algorithm,
                publicKeyArmored,
                encryptedPrivateKeyArmored,
                keyCreatedAt
        );
    }

    private void persist(Long userId, UserPreference preference, MailE2eeKeyProfile current, MailE2eeKeyProfile profile) {
        LocalDateTime now = LocalDateTime.now();
        applyProfile(preference, profile);
        clearRecoveryIfProfileChanged(preference, current, profile);
        preference.setUpdatedAt(now);
        if (preference.getId() == null) {
            preference.setCreatedAt(now);
            preference.setDeleted(0);
            userPreferenceMapper.insert(preference);
            return;
        }
        updateColumns(preference.getId(), userId, profile, now, shouldClearRecovery(current, profile));
    }

    private void applyProfile(UserPreference preference, MailE2eeKeyProfile profile) {
        preference.setMailE2eeEnabled(profile.enabled() ? ENABLED_FLAG : DISABLED_FLAG);
        if (!profile.enabled()) {
            clearProfile(preference);
            return;
        }
        preference.setMailE2eeKeyFingerprint(profile.fingerprint());
        preference.setMailE2eeKeyAlgorithm(profile.algorithm());
        preference.setMailE2eePublicKeyArmored(profile.publicKeyArmored());
        preference.setMailE2eePrivateKeyEncrypted(profile.encryptedPrivateKeyArmored());
        preference.setMailE2eeKeyCreatedAt(profile.keyCreatedAt());
    }

    private void updateColumns(
            Long preferenceId,
            Long userId,
            MailE2eeKeyProfile profile,
            LocalDateTime updatedAt,
            boolean clearRecovery
    ) {
        LambdaUpdateWrapper<UserPreference> update = new LambdaUpdateWrapper<UserPreference>()
                .eq(UserPreference::getId, preferenceId)
                .eq(UserPreference::getOwnerId, userId)
                .set(UserPreference::getMailE2eeEnabled, profile.enabled() ? ENABLED_FLAG : DISABLED_FLAG)
                .set(UserPreference::getUpdatedAt, updatedAt);
        if (!profile.enabled()) {
            clearProfile(update);
            clearRecovery(update);
            userPreferenceMapper.update(null, update);
            return;
        }
        update.set(UserPreference::getMailE2eeKeyFingerprint, profile.fingerprint())
                .set(UserPreference::getMailE2eeKeyAlgorithm, profile.algorithm())
                .set(UserPreference::getMailE2eePublicKeyArmored, profile.publicKeyArmored())
                .set(UserPreference::getMailE2eePrivateKeyEncrypted, profile.encryptedPrivateKeyArmored())
                .set(UserPreference::getMailE2eeKeyCreatedAt, profile.keyCreatedAt());
        if (clearRecovery) {
            clearRecovery(update);
        }
        userPreferenceMapper.update(null, update);
    }

    private void clearProfile(UserPreference preference) {
        preference.setMailE2eeKeyFingerprint(null);
        preference.setMailE2eeKeyAlgorithm(null);
        preference.setMailE2eePublicKeyArmored(null);
        preference.setMailE2eePrivateKeyEncrypted(null);
        preference.setMailE2eeKeyCreatedAt(null);
    }

    private void clearRecoveryIfProfileChanged(
            UserPreference preference,
            MailE2eeKeyProfile current,
            MailE2eeKeyProfile next
    ) {
        if (!shouldClearRecovery(current, next)) {
            return;
        }
        preference.setMailE2eeRecoveryPrivateKeyEncrypted(null);
        preference.setMailE2eeRecoveryUpdatedAt(null);
    }

    private void clearProfile(LambdaUpdateWrapper<UserPreference> update) {
        update.setSql("mail_e2ee_key_fingerprint = null")
                .setSql("mail_e2ee_key_algorithm = null")
                .setSql("mail_e2ee_public_key_armored = null")
                .setSql("mail_e2ee_private_key_encrypted = null")
                .setSql("mail_e2ee_key_created_at = null");
    }

    private void clearRecovery(LambdaUpdateWrapper<UserPreference> update) {
        update.setSql("mail_e2ee_recovery_private_key_encrypted = null")
                .setSql("mail_e2ee_recovery_updated_at = null");
    }

    private boolean shouldClearRecovery(MailE2eeKeyProfile current, MailE2eeKeyProfile next) {
        if (!next.enabled()) {
            return true;
        }
        return current.enabled() && !next.fingerprint().equals(current.fingerprint());
    }

    private String requireText(String candidate, String message) {
        if (!StringUtils.hasText(candidate)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return candidate.trim();
    }

    private String normalizeFingerprint(String fingerprint) {
        String value = requireText(fingerprint, "Mail E2EE fingerprint is required");
        String normalized = value.replace(" ", "").toUpperCase();
        if (normalized.length() < 40 || normalized.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE fingerprint is invalid");
        }
        if (!normalized.matches("[0-9A-F]+")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE fingerprint is invalid");
        }
        return normalized;
    }

    private LocalDateTime resolveKeyCreatedAt(LocalDateTime requestedAt, LocalDateTime currentCreatedAt) {
        if (requestedAt != null) {
            return requestedAt;
        }
        if (currentCreatedAt != null) {
            return currentCreatedAt;
        }
        return LocalDateTime.now();
    }

    private String safeAuditFingerprint(String fingerprint) {
        if (!StringUtils.hasText(fingerprint) || fingerprint.length() < 8) {
            return "none";
        }
        return fingerprint.substring(0, 8);
    }

    private MailE2eeKeyProfileVo toVo(MailE2eeKeyProfile profile) {
        return new MailE2eeKeyProfileVo(
                profile.enabled(),
                profile.fingerprint(),
                profile.algorithm(),
                profile.publicKeyArmored(),
                profile.encryptedPrivateKeyArmored(),
                profile.keyCreatedAt()
        );
    }

    private record MailE2eeKeyProfile(
            boolean enabled,
            String fingerprint,
            String algorithm,
            String publicKeyArmored,
            String encryptedPrivateKeyArmored,
            LocalDateTime keyCreatedAt
    ) {
        private static MailE2eeKeyProfile disabled() {
            return new MailE2eeKeyProfile(false, null, null, null, null, null);
        }
    }
}
