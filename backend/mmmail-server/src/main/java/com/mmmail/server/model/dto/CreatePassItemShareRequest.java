package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePassItemShareRequest(
        @NotBlank @Size(max = 254) String email
) {
}
