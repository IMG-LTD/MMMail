package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MailSummaryVo(
        String id,
        String ownerId,
        String senderEmail,
        String senderDisplayName,
        String senderType,
        boolean isImportantContact,
        boolean hasAttachments,
        String replyState,
        boolean needsReply,
        String latestActor,
        Integer conversationMessageCount,
        String peerEmail,
        String folderType,
        String customFolderId,
        String customFolderName,
        String subject,
        String preview,
        boolean isRead,
        boolean isStarred,
        boolean isDraft,
        LocalDateTime sentAt,
        List<String> labels
) {
}
