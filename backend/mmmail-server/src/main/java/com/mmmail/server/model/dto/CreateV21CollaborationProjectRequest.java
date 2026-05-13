package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateV21CollaborationProjectRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 32) String product,
        @Size(max = 32) String status
) {
}
