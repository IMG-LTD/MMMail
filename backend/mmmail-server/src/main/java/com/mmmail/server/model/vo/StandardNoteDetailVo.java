package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StandardNoteDetailVo(
        String id,
        String title,
        String content,
        String noteType,
        List<String> tags,
        boolean pinned,
        boolean archived,
        int currentVersion,
        String folderId,
        String folderName,
        List<StandardNoteChecklistItemVo> checklistItems,
        int checklistTaskCount,
        int completedChecklistTaskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
