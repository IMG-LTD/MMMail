package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record StandardNoteSummaryVo(
        String id,
        String title,
        String preview,
        String noteType,
        List<String> tags,
        boolean pinned,
        boolean archived,
        int currentVersion,
        String folderId,
        String folderName,
        int checklistTaskCount,
        int completedChecklistTaskCount,
        LocalDateTime updatedAt
) {
}
