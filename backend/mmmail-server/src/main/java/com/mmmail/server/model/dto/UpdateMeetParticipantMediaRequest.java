package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMeetParticipantMediaRequest(
        @NotNull Boolean audioEnabled,
        @NotNull Boolean videoEnabled,
        @NotNull Boolean screenSharing
) {
}
