package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record JoinMeetWaitlistRequest(
        @Size(max = 512) String note
) {
}
