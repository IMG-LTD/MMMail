package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocsNoteSuggestionRequest(
        @Min(0) Integer selectionStart,
        @Min(1) Integer selectionEnd,
        @NotBlank @Size(max = 2000) String originalText,
        @Size(max = 2000) String replacementText,
        @Min(1) Integer baseVersion
) {
}
