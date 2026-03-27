package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitMeetGuestRequestRequest(
        @NotBlank @Size(min = 2, max = 64) String displayName,
        Boolean audioEnabled,
        Boolean videoEnabled
) {
}
