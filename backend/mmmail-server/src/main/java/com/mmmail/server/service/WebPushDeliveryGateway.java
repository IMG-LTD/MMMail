package com.mmmail.server.service;

public interface WebPushDeliveryGateway {

    boolean isConfigured();

    String publicKey();

    String configurationMessage();

    WebPushDeliveryResult send(WebPushDispatchRequest request);

    record WebPushDispatchRequest(
            String endpoint,
            String p256dh,
            String auth,
            String payload
    ) {
    }

    record WebPushDeliveryResult(
            boolean success,
            boolean expired,
            String message
    ) {
    }
}
