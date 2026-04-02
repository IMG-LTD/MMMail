package com.mmmail.server.model.vo;

import java.util.List;

public record MailE2eeRecipientStatusVo(
        String toEmail,
        String fromEmail,
        boolean deliverable,
        boolean encryptionReady,
        String readiness,
        int routeCount,
        List<MailE2eeRecipientRouteVo> routes
) {
}
