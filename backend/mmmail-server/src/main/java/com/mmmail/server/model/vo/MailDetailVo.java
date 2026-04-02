package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MailDetailVo(
        String id,
        String ownerId,
        String senderEmail,
        String peerEmail,
        String folderType,
        String customFolderId,
        String customFolderName,
        String subject,
        String body,
        boolean isRead,
        boolean isStarred,
        boolean isDraft,
        LocalDateTime sentAt,
        List<String> labels,
        List<MailAttachmentVo> attachments,
        MailBodyE2eeVo e2ee
) {
}
