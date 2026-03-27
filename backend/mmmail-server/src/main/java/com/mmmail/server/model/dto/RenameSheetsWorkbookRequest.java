package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSheetsWorkbookRequest(
        @NotBlank @Size(max = 255) String title
) {
}
