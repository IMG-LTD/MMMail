package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLumoProjectRequest(
        @NotBlank @Size(min = 2, max = 64) String name,
        @Size(max = 256) String description
) {
}
