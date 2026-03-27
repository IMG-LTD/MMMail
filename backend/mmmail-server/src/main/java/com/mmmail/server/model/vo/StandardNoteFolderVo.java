package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record StandardNoteFolderVo(
        String id,
        String name,
        String color,
        String description,
        int noteCount,
        int checklistTaskCount,
        int completedChecklistTaskCount,
        LocalDateTime updatedAt
) {
}
