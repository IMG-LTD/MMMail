package com.mmmail.server.model.vo;

import java.util.List;

public record CollabAwarenessVo(
        String resourceType,
        String resourceId,
        List<DocsNotePresenceVo> activeUsers
) {
}
