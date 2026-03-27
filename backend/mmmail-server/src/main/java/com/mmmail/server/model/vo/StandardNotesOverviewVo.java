package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record StandardNotesOverviewVo(
        long totalNoteCount,
        long activeNoteCount,
        long pinnedNoteCount,
        long archivedNoteCount,
        long uniqueTagCount,
        long folderCount,
        long checklistNoteCount,
        long checklistTaskCount,
        long completedChecklistTaskCount,
        boolean exportReady,
        LocalDateTime generatedAt
) {
}
