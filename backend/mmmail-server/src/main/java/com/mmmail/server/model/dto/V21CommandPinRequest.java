package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

public record V21CommandPinRequest(
        @NotBlank String commandId,
        boolean pinned
) {
}
