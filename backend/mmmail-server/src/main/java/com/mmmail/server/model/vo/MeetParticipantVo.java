package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetParticipantVo(
        String participantId,
        String roomId,
        String userId,
        String displayName,
        String role,
        String status,
        boolean audioEnabled,
        boolean videoEnabled,
        boolean screenSharing,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        LocalDateTime lastHeartbeatAt,
        boolean self,
        boolean canManageParticipants,
        boolean canTransferHost
) {
}
