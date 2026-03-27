package com.mmmail.server.model.vo;

public record MailDeliveryTarget(
        Long ownerId,
        String targetEmail,
        String forwardToEmail
) {
}
