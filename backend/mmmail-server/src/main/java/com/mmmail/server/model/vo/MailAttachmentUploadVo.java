package com.mmmail.server.model.vo;

public record MailAttachmentUploadVo(
        String draftId,
        MailAttachmentVo attachment
) {
}
