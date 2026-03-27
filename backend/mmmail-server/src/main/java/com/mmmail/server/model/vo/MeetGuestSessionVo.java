package com.mmmail.server.model.vo;

import java.util.List;

public record MeetGuestSessionVo(
        String roomId,
        String roomCode,
        String topic,
        String sessionStatus,
        MeetGuestParticipantViewVo selfParticipant,
        List<MeetGuestParticipantViewVo> participants
) {
}
