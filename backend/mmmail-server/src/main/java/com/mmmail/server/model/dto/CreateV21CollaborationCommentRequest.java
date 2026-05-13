package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateV21CollaborationCommentRequest(
        @NotBlank @Size(max = 4000) String body
) {
}
