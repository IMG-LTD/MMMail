package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocsNoteCommentRequest(
        @Size(max = 512) String excerpt,
        @NotBlank @Size(max = 2000) String content
) {
}
