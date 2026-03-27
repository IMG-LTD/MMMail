package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetGuestRequestVo(
        String requestId,
        String roomId,
        String roomCode,
        String roomStatus,
        String displayName,
        boolean audioEnabled,
        boolean videoEnabled,
        String status,
        String requestToken,
        String guestSessionToken,
        String participantId,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt
) {
}
