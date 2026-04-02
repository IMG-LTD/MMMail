package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.MailDeliveryTarget;
import com.mmmail.server.model.vo.MailE2eeRecipientRouteVo;
import com.mmmail.server.model.vo.MailE2eeRecipientStatusVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class MailE2eeRecipientDiscoveryService {

    private static final String READY = "READY";
    private static final String NOT_READY = "NOT_READY";
    private static final String UNDELIVERABLE = "UNDELIVERABLE";
    private static final int ENABLED_FLAG = 1;

    private final MailSenderIdentityService mailSenderIdentityService;
    private final MailDeliveryRouteService mailDeliveryRouteService;
    private final UserPreferenceMapper userPreferenceMapper;

    public MailE2eeRecipientDiscoveryService(
            MailSenderIdentityService mailSenderIdentityService,
            MailDeliveryRouteService mailDeliveryRouteService,
            UserPreferenceMapper userPreferenceMapper
    ) {
        this.mailSenderIdentityService = mailSenderIdentityService;
        this.mailDeliveryRouteService = mailDeliveryRouteService;
        this.userPreferenceMapper = userPreferenceMapper;
    }

    public MailE2eeRecipientStatusVo preview(Long userId, String toEmail, String fromEmail) {
        String senderEmail = mailSenderIdentityService.resolveAuthorizedSenderEmail(userId, fromEmail);
        String normalizedTarget = normalizeEmail(toEmail);
        List<MailDeliveryTarget> targets = mailDeliveryRouteService.previewDeliveryTargets(userId, senderEmail, normalizedTarget);
        if (targets.isEmpty()) {
            return new MailE2eeRecipientStatusVo(normalizedTarget, senderEmail, false, false, UNDELIVERABLE, 0, List.of());
        }
        List<MailE2eeRecipientRouteVo> routes = targets.stream().map(this::toRouteVo).toList();
        boolean encryptionReady = routes.stream().allMatch(MailE2eeRecipientRouteVo::keyAvailable);
        return new MailE2eeRecipientStatusVo(
                normalizedTarget,
                senderEmail,
                true,
                encryptionReady,
                encryptionReady ? READY : NOT_READY,
                routes.size(),
                routes
        );
    }

    private MailE2eeRecipientRouteVo toRouteVo(MailDeliveryTarget target) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, target.ownerId())
                .last("limit 1"));
        if (!hasEnabledKeyProfile(preference)) {
            return new MailE2eeRecipientRouteVo(target.targetEmail(), target.forwardToEmail(), false, null, null, null);
        }
        return new MailE2eeRecipientRouteVo(
                target.targetEmail(),
                target.forwardToEmail(),
                true,
                preference.getMailE2eeKeyFingerprint(),
                preference.getMailE2eeKeyAlgorithm(),
                preference.getMailE2eePublicKeyArmored()
        );
    }

    private boolean hasEnabledKeyProfile(UserPreference preference) {
        return preference != null
                && preference.getMailE2eeEnabled() != null
                && preference.getMailE2eeEnabled() == ENABLED_FLAG
                && StringUtils.hasText(preference.getMailE2eePublicKeyArmored())
                && StringUtils.hasText(preference.getMailE2eeKeyFingerprint());
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
