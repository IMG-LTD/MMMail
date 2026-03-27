package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MailFilterVo(
        String id,
        String name,
        boolean enabled,
        String senderContains,
        String subjectContains,
        String keywordContains,
        String targetFolder,
        String targetCustomFolderId,
        String targetCustomFolderName,
        List<String> labels,
        boolean markRead,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
