package com.mmmail.server.model.vo;

public record MeetGuestParticipantViewVo(
        String participantId,
        String displayName,
        String role,
        String status,
        boolean audioEnabled,
        boolean videoEnabled,
        boolean screenSharing,
        boolean self
) {
}
