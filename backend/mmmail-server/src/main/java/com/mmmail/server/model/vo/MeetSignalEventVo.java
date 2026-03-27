package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetSignalEventVo(
        String eventSeq,
        String roomId,
        String signalType,
        String fromParticipantId,
        String toParticipantId,
        String payload,
        LocalDateTime createdAt
) {
}
