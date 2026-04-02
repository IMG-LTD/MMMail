package com.mmmail.server.model.vo;

import java.util.List;

public record MailBodyE2eeVo(
        boolean enabled,
        String algorithm,
        List<String> recipientFingerprints
) {
}
