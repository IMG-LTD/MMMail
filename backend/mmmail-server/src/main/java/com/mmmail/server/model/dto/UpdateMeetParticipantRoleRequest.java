package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMeetParticipantRoleRequest(
        @NotBlank
        @Pattern(regexp = "HOST|CO_HOST|PARTICIPANT", message = "role must be HOST, CO_HOST or PARTICIPANT")
        String role
) {
}
