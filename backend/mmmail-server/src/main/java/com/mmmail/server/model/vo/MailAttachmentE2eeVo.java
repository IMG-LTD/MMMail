package com.mmmail.server.model.vo;

import java.util.List;

public record MailAttachmentE2eeVo(
        boolean enabled,
        String algorithm,
        List<String> recipientFingerprints
) {
}
