package com.mmmail.server.model.vo;

public record MailAttachmentVo(
        String id,
        String mailId,
        String fileName,
        String contentType,
        long fileSize,
        MailAttachmentE2eeVo e2ee
) {
}
