package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MeetPruneInactiveResultVo(
        String roomId,
        int inactiveSeconds,
        int removedCount,
        List<String> removedParticipantIds,
        LocalDateTime executedAt
) {
}
