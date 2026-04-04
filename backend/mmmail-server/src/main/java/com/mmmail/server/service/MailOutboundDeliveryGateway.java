package com.mmmail.server.service;

public interface MailOutboundDeliveryGateway {

    boolean isConfigured();

    String configurationMessage();

    MailOutboundDeliveryResult send(MailOutboundRequest request);

    record MailOutboundRequest(
            String fromEmail,
            String toEmail,
            String subject,
            String body
    ) {
    }

    record MailOutboundDeliveryResult(
            boolean success,
            String message
    ) {
    }
}
