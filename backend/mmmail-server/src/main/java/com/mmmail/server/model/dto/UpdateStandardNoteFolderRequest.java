package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStandardNoteFolderRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 7) String color,
        @Size(max = 160) String description
) {
}
