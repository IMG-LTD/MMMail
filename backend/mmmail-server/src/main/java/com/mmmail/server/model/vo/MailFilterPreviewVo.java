package com.mmmail.server.model.vo;

import java.util.List;

public record MailFilterPreviewVo(
        String senderEmail,
        String subject,
        String baseFolder,
        String effectiveFolder,
        String effectiveCustomFolderId,
        String effectiveCustomFolderName,
        List<String> effectiveLabels,
        boolean markRead,
        boolean blockedBySecurityRule,
        String securityReason,
        String securityMatchedRule,
        String matchedFilterId,
        String matchedFilterName
) {
}
