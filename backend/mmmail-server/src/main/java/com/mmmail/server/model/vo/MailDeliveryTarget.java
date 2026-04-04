package com.mmmail.server.model.vo;

public record MailDeliveryTarget(
        Long ownerId,
        String targetEmail,
        String forwardToEmail
) {

    public static MailDeliveryTarget internal(Long ownerId, String targetEmail, String forwardToEmail) {
        return new MailDeliveryTarget(ownerId, targetEmail, forwardToEmail);
    }

    public static MailDeliveryTarget smtp(String targetEmail) {
        return new MailDeliveryTarget(null, targetEmail, targetEmail);
    }

    public boolean isInternalMailbox() {
        return ownerId != null;
    }

    public boolean isSmtpOutbound() {
        return ownerId == null;
    }
}
