package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PruneMeetInactiveParticipantsRequest(
        @NotNull @Min(30) @Max(86400) Integer inactiveSeconds
) {
}
