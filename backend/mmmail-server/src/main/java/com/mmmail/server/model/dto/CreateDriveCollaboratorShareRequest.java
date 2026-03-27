package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDriveCollaboratorShareRequest(
        @NotBlank @Email @Size(max = 254) String targetEmail,
        @NotBlank @Pattern(regexp = "VIEW|EDIT") String permission
) {
}
