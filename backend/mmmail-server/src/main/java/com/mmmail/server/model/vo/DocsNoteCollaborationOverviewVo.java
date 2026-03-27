package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record DocsNoteCollaborationOverviewVo(
        LocalDateTime generatedAt,
        List<DocsNoteShareVo> collaborators,
        List<DocsNoteCommentVo> comments,
        List<DocsNotePresenceVo> activeSessions,
        long syncCursor,
        String syncVersion
) {
}
