package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMeetSignalRequest(
        @NotNull Long fromParticipantId,
        Long toParticipantId,
        @NotBlank @Size(max = 8192) String payload
) {
}
