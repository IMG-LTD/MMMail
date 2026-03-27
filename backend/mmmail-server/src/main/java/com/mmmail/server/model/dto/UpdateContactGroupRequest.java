package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContactGroupRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 256) String description
) {
}
