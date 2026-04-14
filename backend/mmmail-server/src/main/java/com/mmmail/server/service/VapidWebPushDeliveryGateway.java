package com.mmmail.server.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Security;

@Component
public class VapidWebPushDeliveryGateway implements WebPushDeliveryGateway {

    private static final String CONFIG_ERROR = "Web Push VAPID configuration is incomplete";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String publicKey;
    private final String configurationMessage;
    private final PushService pushService;

    public VapidWebPushDeliveryGateway(
            @Value("${mmmail.web-push.vapid-public-key:}") String publicKey,
            @Value("${mmmail.web-push.vapid-private-key:}") String privateKey,
            @Value("${mmmail.web-push.vapid-subject:}") String subject
    ) {
        this.publicKey = normalize(publicKey);
        String normalizedPrivateKey = normalize(privateKey);
        String normalizedSubject = normalize(subject);
        PushService createdService = null;
        String initMessage = null;
        if (!StringUtils.hasText(this.publicKey)
                || !StringUtils.hasText(normalizedPrivateKey)
                || !StringUtils.hasText(normalizedSubject)) {
            initMessage = CONFIG_ERROR;
        } else {
            try {
                createdService = new PushService(this.publicKey, normalizedPrivateKey, normalizedSubject);
            } catch (Exception exception) {
                initMessage = exception.getMessage() == null ? CONFIG_ERROR : exception.getMessage();
            }
        }
        this.pushService = createdService;
        this.configurationMessage = initMessage;
    }

    @Override
    public boolean isConfigured() {
        return pushService != null;
    }

    @Override
    public String publicKey() {
        return publicKey;
    }

    @Override
    public String configurationMessage() {
        return configurationMessage;
    }

    @Override
    public WebPushDeliveryResult send(WebPushDispatchRequest request) {
        if (pushService == null) {
            return new WebPushDeliveryResult(false, false, configurationMessage);
        }
        try {
            Notification notification = new Notification(
                    new Subscription(
                            request.endpoint(),
                            new Subscription.Keys(request.p256dh(), request.auth())
                    ),
                    request.payload()
            );
            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusMessage = response.getStatusLine().toString();
            if (statusCode >= 200 && statusCode < 300) {
                return new WebPushDeliveryResult(true, false, statusMessage);
            }
            return new WebPushDeliveryResult(false, statusCode == 404 || statusCode == 410, statusMessage);
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            return new WebPushDeliveryResult(false, false, message);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
