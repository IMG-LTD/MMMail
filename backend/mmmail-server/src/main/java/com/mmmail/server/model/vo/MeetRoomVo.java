package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetRoomVo(
        String roomId,
        String roomCode,
        String topic,
        String accessLevel,
        int maxParticipants,
        String joinCode,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long durationSeconds
) {
}
