package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RespondDriveCollaboratorShareRequest(
        @NotBlank @Pattern(regexp = "ACCEPT|DECLINE") String response
) {
}
