package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetAccessOverviewVo(
        String planCode,
        String planName,
        boolean eligibleForInstantAccess,
        boolean accessGranted,
        boolean waitlistRequested,
        boolean salesContactRequested,
        String accessState,
        String recommendedAction,
        String companyName,
        Integer requestedSeats,
        String requestNote,
        LocalDateTime waitlistRequestedAt,
        LocalDateTime accessGrantedAt,
        LocalDateTime salesContactRequestedAt
) {
}
