package com.mmmail.server.model.vo;

public record MailPublicSecureAttachmentVo(
        String id,
        String fileName,
        String contentType,
        long fileSize,
        String algorithm
) {
}
