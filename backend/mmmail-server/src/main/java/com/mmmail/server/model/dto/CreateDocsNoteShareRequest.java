package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDocsNoteShareRequest(
        @NotBlank @Size(max = 254) String collaboratorEmail,
        @NotBlank @Pattern(regexp = "VIEW|EDIT") String permission
) {
}
