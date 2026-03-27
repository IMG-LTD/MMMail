package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateDocsNoteRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 100000) String content,
        @Min(1) Integer currentVersion
) {
}
