package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePassSharedVaultRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description
) {
}
