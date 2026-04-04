package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.WebPushSubscriptionMapper;
import com.mmmail.server.model.dto.DeleteSuiteWebPushSubscriptionRequest;
import com.mmmail.server.model.dto.RegisterSuiteWebPushSubscriptionRequest;
import com.mmmail.server.model.entity.WebPushSubscription;
import com.mmmail.server.model.vo.SuiteWebPushStatusVo;
import com.mmmail.server.model.vo.SuiteWebPushSubscriptionVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 255;
    private static final String DELIVERY_SCOPE = "MAIL_INBOX";
    private static final String DEFAULT_PUSH_TITLE = "New mail";

    private final WebPushSubscriptionMapper webPushSubscriptionMapper;
    private final WebPushDeliveryGateway webPushDeliveryGateway;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WebPushService(
            WebPushSubscriptionMapper webPushSubscriptionMapper,
            WebPushDeliveryGateway webPushDeliveryGateway,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.webPushSubscriptionMapper = webPushSubscriptionMapper;
        this.webPushDeliveryGateway = webPushDeliveryGateway;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public SuiteWebPushStatusVo getStatus(Long userId, String ipAddress) {
        auditService.record(userId, "SUITE_WEB_PUSH_STATUS_QUERY", "configured=" + webPushDeliveryGateway.isConfigured(), ipAddress);
        return new SuiteWebPushStatusVo(
                webPushDeliveryGateway.isConfigured(),
                DELIVERY_SCOPE,
                webPushDeliveryGateway.publicKey(),
                webPushDeliveryGateway.configurationMessage(),
                countActiveSubscriptions(userId)
        );
    }

    public SuiteWebPushSubscriptionVo registerSubscription(
            Long userId,
            RegisterSuiteWebPushSubscriptionRequest request,
            String ipAddress
    ) {
        requireConfigured();
        String endpoint = request.endpoint().trim();
        String endpointHash = hashEndpoint(endpoint);
        LocalDateTime now = LocalDateTime.now();
        WebPushSubscription subscription = findByOwnerAndHash(userId, endpointHash);
        if (subscription == null) {
            subscription = new WebPushSubscription();
            subscription.setOwnerId(userId);
            subscription.setEndpointHash(endpointHash);
            subscription.setCreatedAt(now);
            subscription.setDeleted(0);
        }
        subscription.setEndpoint(endpoint);
        subscription.setP256dhKey(request.p256dh().trim());
        subscription.setAuthKey(request.auth().trim());
        subscription.setContentEncoding(request.contentEncoding().trim());
        subscription.setUserAgent(normalizeOptional(request.userAgent()));
        subscription.setUpdatedAt(now);
        if (subscription.getId() == null) {
            webPushSubscriptionMapper.insert(subscription);
        } else {
            webPushSubscriptionMapper.updateById(subscription);
        }
        auditService.record(userId, "SUITE_WEB_PUSH_SUBSCRIPTION_REGISTER", endpointHash, ipAddress);
        return toVo(subscription);
    }

    public boolean deleteSubscription(
            Long userId,
            DeleteSuiteWebPushSubscriptionRequest request,
            String ipAddress
    ) {
        String endpointHash = hashEndpoint(request.endpoint().trim());
        WebPushSubscription subscription = findByOwnerAndHash(userId, endpointHash);
        if (subscription == null) {
            return false;
        }
        auditService.record(userId, "SUITE_WEB_PUSH_SUBSCRIPTION_DELETE", endpointHash, ipAddress);
        return webPushSubscriptionMapper.deleteById(subscription.getId()) > 0;
    }

    public void dispatchInboxMail(Long userId, Long mailId, String senderEmail, String subject) {
        List<WebPushSubscription> subscriptions = listActiveSubscriptions(userId);
        if (subscriptions.isEmpty()) {
            return;
        }
        if (!webPushDeliveryGateway.isConfigured()) {
            markBatchFailure(subscriptions, webPushDeliveryGateway.configurationMessage());
            return;
        }
        String payload = buildPayload(mailId, senderEmail, subject);
        for (WebPushSubscription subscription : subscriptions) {
            WebPushDeliveryGateway.WebPushDeliveryResult result = webPushDeliveryGateway.send(
                    new WebPushDeliveryGateway.WebPushDispatchRequest(
                            subscription.getEndpoint(),
                            subscription.getP256dhKey(),
                            subscription.getAuthKey(),
                            payload
                    )
            );
            if (result.success()) {
                markSuccess(subscription);
                continue;
            }
            markFailure(subscription, result.message());
            if (result.expired()) {
                webPushSubscriptionMapper.deleteById(subscription.getId());
            }
            log.warn("Web Push delivery failed for subscription {}: {}", subscription.getEndpointHash(), result.message());
        }
    }

    private void requireConfigured() {
        if (webPushDeliveryGateway.isConfigured()) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, webPushDeliveryGateway.configurationMessage());
    }

    private int countActiveSubscriptions(Long userId) {
        return Math.toIntExact(webPushSubscriptionMapper.selectCount(new LambdaQueryWrapper<WebPushSubscription>()
                .eq(WebPushSubscription::getOwnerId, userId)));
    }

    private WebPushSubscription findByOwnerAndHash(Long userId, String endpointHash) {
        return webPushSubscriptionMapper.selectOne(new LambdaQueryWrapper<WebPushSubscription>()
                .eq(WebPushSubscription::getOwnerId, userId)
                .eq(WebPushSubscription::getEndpointHash, endpointHash));
    }

    private List<WebPushSubscription> listActiveSubscriptions(Long userId) {
        return webPushSubscriptionMapper.selectList(new LambdaQueryWrapper<WebPushSubscription>()
                .eq(WebPushSubscription::getOwnerId, userId));
    }

    private void markBatchFailure(List<WebPushSubscription> subscriptions, String message) {
        for (WebPushSubscription subscription : subscriptions) {
            markFailure(subscription, message);
        }
    }

    private void markSuccess(WebPushSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        subscription.setLastSuccessAt(now);
        subscription.setLastFailureAt(null);
        subscription.setLastErrorMessage(null);
        subscription.setUpdatedAt(now);
        webPushSubscriptionMapper.updateById(subscription);
    }

    private void markFailure(WebPushSubscription subscription, String message) {
        LocalDateTime now = LocalDateTime.now();
        subscription.setLastFailureAt(now);
        subscription.setLastErrorMessage(trimError(message));
        subscription.setUpdatedAt(now);
        webPushSubscriptionMapper.updateById(subscription);
    }

    private String buildPayload(Long mailId, String senderEmail, String subject) {
        try {
            return objectMapper.writeValueAsString(new MailPushPayload(
                    "mail-inbox",
                    DEFAULT_PUSH_TITLE,
                    buildBody(senderEmail, subject),
                    "mail-" + mailId,
                    mailId == null ? "/inbox" : "/mail/" + mailId,
                    mailId
            ));
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize Web Push payload");
        }
    }

    private String buildBody(String senderEmail, String subject) {
        String normalizedSender = StringUtils.hasText(senderEmail) ? senderEmail.trim() : "Unknown sender";
        if (!StringUtils.hasText(subject)) {
            return "New mail from " + normalizedSender;
        }
        return normalizedSender + " · " + subject.trim();
    }

    private String hashEndpoint(String endpoint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(endpoint.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown";
        }
        String normalized = message.trim();
        if (normalized.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private SuiteWebPushSubscriptionVo toVo(WebPushSubscription subscription) {
        return new SuiteWebPushSubscriptionVo(
                subscription.getId(),
                subscription.getEndpointHash(),
                subscription.getLastSuccessAt(),
                subscription.getLastFailureAt(),
                subscription.getLastErrorMessage(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    private record MailPushPayload(
            String kind,
            String title,
            String body,
            String tag,
            String routePath,
            Long mailId
    ) {
    }
}
