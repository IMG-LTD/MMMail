package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;

public record ResolveDocsNoteSuggestionRequest(
        @Min(1) Integer currentVersion
) {
}
