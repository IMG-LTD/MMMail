package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSheetsWorkbookShareRequest(
        @NotBlank @Pattern(regexp = "VIEW|EDIT") String permission
) {
}
