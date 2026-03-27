package com.mmmail.server.model.vo;

public record MailAttachmentDownloadVo(
        String fileName,
        String contentType,
        byte[] content
) {
}
