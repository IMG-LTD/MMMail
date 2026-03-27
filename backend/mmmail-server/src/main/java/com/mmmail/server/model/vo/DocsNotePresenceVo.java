package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record DocsNotePresenceVo(
        String presenceId,
        String userId,
        String email,
        String displayName,
        String sessionId,
        String activeMode,
        String permission,
        LocalDateTime lastHeartbeatAt
) {
}
