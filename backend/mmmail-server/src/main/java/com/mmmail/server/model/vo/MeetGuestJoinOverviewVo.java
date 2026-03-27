package com.mmmail.server.model.vo;

public record MeetGuestJoinOverviewVo(
        String roomId,
        String roomCode,
        String topic,
        String joinCode,
        String accessLevel,
        String roomStatus,
        boolean guestJoinEnabled,
        boolean lobbyEnabled,
        int activeParticipants,
        int maxParticipants
) {
}
