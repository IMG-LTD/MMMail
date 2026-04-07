package com.mmmail.server.model.vo;

public record MailE2eeRecipientRouteVo(
        String targetEmail,
        String forwardToEmail,
        boolean smtpOutbound,
        boolean keyAvailable,
        String fingerprint,
        String algorithm,
        String publicKeyArmored
) {
}
