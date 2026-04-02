package com.mmmail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.MailBodyE2eePayloadRequest;
import com.mmmail.server.model.dto.SendMailRequest;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.MailBodyE2eeVo;
import com.mmmail.server.model.vo.MailE2eeRecipientRouteVo;
import com.mmmail.server.model.vo.MailE2eeRecipientStatusVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MailE2eeMessageService {

    private static final int DISABLED_FLAG = 0;
    private static final int ENABLED_FLAG = 1;
    private static final String ENCRYPTED_PREVIEW = "Mail E2EE encrypted body";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final MailE2eeRecipientDiscoveryService recipientDiscoveryService;
    private final UserPreferenceMapper userPreferenceMapper;
    private final ObjectMapper objectMapper;

    public MailE2eeMessageService(
            MailE2eeRecipientDiscoveryService recipientDiscoveryService,
            UserPreferenceMapper userPreferenceMapper,
            ObjectMapper objectMapper
    ) {
        this.recipientDiscoveryService = recipientDiscoveryService;
        this.userPreferenceMapper = userPreferenceMapper;
        this.objectMapper = objectMapper;
    }

    public OutboundBody resolveOutboundBody(Long userId, String senderEmail, SendMailRequest request) {
        if (request.e2ee() == null) {
            return OutboundBody.plain(requirePlainBody(request.body()));
        }
        rejectPlainBodyLeak(request.body());
        return resolveEncryptedBody(userId, senderEmail, request.toEmail(), request.e2ee());
    }

    public MailBodyE2eeVo toDetailVo(MailMessage message) {
        if (!isEncrypted(message)) {
            return null;
        }
        return new MailBodyE2eeVo(true, message.getBodyE2eeAlgorithm(), parseFingerprints(message.getBodyE2eeFingerprintsJson()));
    }

    public String resolvePreview(MailMessage message) {
        if (isEncrypted(message)) {
            return ENCRYPTED_PREVIEW;
        }
        String body = message.getBodyCiphertext() == null ? "" : message.getBodyCiphertext();
        return body.length() > 80 ? body.substring(0, 80) + "..." : body;
    }

    private OutboundBody resolveEncryptedBody(
            Long userId,
            String senderEmail,
            String toEmail,
            MailBodyE2eePayloadRequest payload
    ) {
        String encryptedBody = requireText(payload.encryptedBody(), "Encrypted Mail E2EE body is required");
        String algorithm = requireText(payload.algorithm(), "Mail E2EE message algorithm is required");
        MailE2eeRecipientStatusVo recipientStatus = recipientDiscoveryService.preview(userId, toEmail, senderEmail);
        requireReadyRecipient(recipientStatus);
        Set<String> expectedFingerprints = buildExpectedFingerprints(userId, recipientStatus);
        List<String> actualFingerprints = normalizeFingerprints(payload.recipientFingerprints());
        if (!expectedFingerprints.equals(new LinkedHashSet<>(actualFingerprints))) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE recipient fingerprints do not match current delivery routes");
        }
        return OutboundBody.encrypted(encryptedBody, algorithm, serializeFingerprints(actualFingerprints));
    }

    private void requireReadyRecipient(MailE2eeRecipientStatusVo recipientStatus) {
        if (!recipientStatus.deliverable() || !recipientStatus.encryptionReady()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Recipient route is not ready for Mail E2EE send");
        }
    }

    private Set<String> buildExpectedFingerprints(Long userId, MailE2eeRecipientStatusVo recipientStatus) {
        LinkedHashSet<String> fingerprints = new LinkedHashSet<>();
        recipientStatus.routes().stream()
                .map(MailE2eeRecipientRouteVo::fingerprint)
                .map(this::normalizeFingerprint)
                .forEach(fingerprints::add);
        fingerprints.add(loadSenderFingerprint(userId));
        return fingerprints;
    }

    private String loadSenderFingerprint(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getOwnerId, userId)
                        .last("limit 1")
        );
        if (!hasEnabledKeyProfile(preference)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Current account does not have a Mail E2EE key profile");
        }
        return normalizeFingerprint(preference.getMailE2eeKeyFingerprint());
    }

    private boolean hasEnabledKeyProfile(UserPreference preference) {
        return preference != null
                && preference.getMailE2eeEnabled() != null
                && preference.getMailE2eeEnabled() == ENABLED_FLAG
                && StringUtils.hasText(preference.getMailE2eeKeyFingerprint())
                && StringUtils.hasText(preference.getMailE2eePublicKeyArmored());
    }

    private String serializeFingerprints(List<String> fingerprints) {
        try {
            return objectMapper.writeValueAsString(fingerprints);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize Mail E2EE fingerprints");
        }
    }

    private List<String> parseFingerprints(String fingerprintsJson) {
        if (!StringUtils.hasText(fingerprintsJson)) {
            throw new IllegalStateException("Encrypted mail is missing Mail E2EE fingerprint metadata");
        }
        try {
            List<String> fingerprints = objectMapper.readValue(fingerprintsJson, STRING_LIST_TYPE);
            if (fingerprints == null || fingerprints.isEmpty()) {
                throw new IllegalStateException("Encrypted mail is missing Mail E2EE fingerprint metadata");
            }
            return fingerprints;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Mail E2EE fingerprint metadata", exception);
        }
    }

    private List<String> normalizeFingerprints(List<String> fingerprints) {
        if (fingerprints == null || fingerprints.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE recipient fingerprints are required");
        }
        return fingerprints.stream().map(this::normalizeFingerprint).distinct().toList();
    }

    private void rejectPlainBodyLeak(String body) {
        if (StringUtils.hasText(body)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Plain body must be omitted when Mail E2EE payload is provided");
        }
    }

    private String requirePlainBody(String body) {
        return requireText(body, "Mail body is required");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private String normalizeFingerprint(String fingerprint) {
        String normalized = requireText(fingerprint, "Mail E2EE fingerprint is required")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
        if (!normalized.matches("[0-9A-F]{40,64}")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail E2EE fingerprint is invalid");
        }
        return normalized;
    }

    private boolean isEncrypted(MailMessage message) {
        return message.getBodyE2eeEnabled() != null && message.getBodyE2eeEnabled() == ENABLED_FLAG;
    }

    public record OutboundBody(
            String bodyCiphertext,
            int bodyE2eeEnabled,
            String bodyE2eeAlgorithm,
            String bodyE2eeFingerprintsJson
    ) {

        private static OutboundBody plain(String body) {
            return new OutboundBody(body, DISABLED_FLAG, null, null);
        }

        private static OutboundBody encrypted(String body, String algorithm, String fingerprintsJson) {
            return new OutboundBody(body, ENABLED_FLAG, algorithm, fingerprintsJson);
        }
    }
}
