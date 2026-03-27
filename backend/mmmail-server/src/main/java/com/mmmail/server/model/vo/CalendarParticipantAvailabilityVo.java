package com.mmmail.server.model.vo;

import java.util.List;

public record CalendarParticipantAvailabilityVo(
        String email,
        String availability,
        int overlapCount,
        List<CalendarAvailabilitySlotVo> busySlots
) {
}
