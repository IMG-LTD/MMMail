package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleStandardNoteChecklistItemRequest(
        @NotNull Integer currentVersion,
        @NotNull Boolean completed
) {
}
