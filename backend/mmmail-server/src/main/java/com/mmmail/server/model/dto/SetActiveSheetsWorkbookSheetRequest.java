package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetActiveSheetsWorkbookSheetRequest(
        @NotBlank @Size(max = 64) String sheetId
) {
}
