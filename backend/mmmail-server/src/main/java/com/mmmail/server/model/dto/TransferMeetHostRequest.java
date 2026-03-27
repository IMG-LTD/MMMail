package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

public record TransferMeetHostRequest(
        @NotNull Long targetParticipantId
) {
}
