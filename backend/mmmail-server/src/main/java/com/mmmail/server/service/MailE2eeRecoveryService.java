package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.UpdateMailE2eeRecoveryRequest;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.MailE2eeRecoveryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MailE2eeRecoveryService {

    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditService auditService;

    public MailE2eeRecoveryService(
            UserPreferenceMapper userPreferenceMapper,
            AuditService auditService
    ) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditService = auditService;
    }

    public MailE2eeRecoveryVo get(Long userId, String ipAddress) {
        MailE2eeRecoveryVo recovery = resolve(loadPreference(userId));
        auditService.record(userId, "MAIL_E2EE_RECOVERY_GET", "enabled=" + recovery.enabled(), ipAddress);
        return recovery;
    }

    @Transactional
    public MailE2eeRecoveryVo update(Long userId, UpdateMailE2eeRecoveryRequest request, String ipAddress) {
        UserPreference preference = requireEnabledKeyProfile(loadPreference(userId));
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(request.enabled())) {
            clearRecovery(preference, now);
            persist(preference);
            auditService.record(userId, "MAIL_E2EE_RECOVERY_DISABLE", "enabled=false", ipAddress);
            return resolve(preference);
        }
        preference.setMailE2eeRecoveryPrivateKeyEncrypted(requireCiphertext(request.encryptedPrivateKeyArmored()));
        preference.setMailE2eeRecoveryUpdatedAt(now);
        preference.setUpdatedAt(now);
        persist(preference);
        auditService.record(userId, "MAIL_E2EE_RECOVERY_UPDATE", "enabled=true", ipAddress);
        return resolve(preference);
    }

    private UserPreference loadPreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId)
                .last("limit 1"));
        if (preference == null) {
            UserPreference created = new UserPreference();
            created.setOwnerId(userId);
            created.setMailE2eeEnabled(0);
            return created;
        }
        return preference;
    }

    private UserPreference requireEnabledKeyProfile(UserPreference preference) {
        if (!hasEnabledKeyProfile(preference)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Current account does not have a Mail E2EE key profile");
        }
        return preference;
    }

    private boolean hasEnabledKeyProfile(UserPreference preference) {
        return preference.getMailE2eeEnabled() != null
                && preference.getMailE2eeEnabled() == 1
                && StringUtils.hasText(preference.getMailE2eePublicKeyArmored())
                && StringUtils.hasText(preference.getMailE2eePrivateKeyEncrypted())
                && StringUtils.hasText(preference.getMailE2eeKeyFingerprint());
    }

    private String requireCiphertext(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE recovery package is required");
        }
        return value.trim();
    }

    private void clearRecovery(UserPreference preference, LocalDateTime updatedAt) {
        preference.setMailE2eeRecoveryPrivateKeyEncrypted(null);
        preference.setMailE2eeRecoveryUpdatedAt(null);
        preference.setUpdatedAt(updatedAt);
    }

    private void persist(UserPreference preference) {
        LambdaUpdateWrapper<UserPreference> update = new LambdaUpdateWrapper<UserPreference>()
                .eq(UserPreference::getId, preference.getId())
                .eq(UserPreference::getOwnerId, preference.getOwnerId())
                .set(UserPreference::getMailE2eeRecoveryPrivateKeyEncrypted, preference.getMailE2eeRecoveryPrivateKeyEncrypted())
                .set(UserPreference::getMailE2eeRecoveryUpdatedAt, preference.getMailE2eeRecoveryUpdatedAt())
                .set(UserPreference::getUpdatedAt, preference.getUpdatedAt());
        userPreferenceMapper.update(null, update);
    }

    private MailE2eeRecoveryVo resolve(UserPreference preference) {
        boolean enabled = hasEnabledKeyProfile(preference)
                && StringUtils.hasText(preference.getMailE2eeRecoveryPrivateKeyEncrypted());
        if (!enabled) {
            return new MailE2eeRecoveryVo(false, null, null);
        }
        return new MailE2eeRecoveryVo(
                true,
                preference.getMailE2eeRecoveryPrivateKeyEncrypted(),
                preference.getMailE2eeRecoveryUpdatedAt()
        );
    }
}
