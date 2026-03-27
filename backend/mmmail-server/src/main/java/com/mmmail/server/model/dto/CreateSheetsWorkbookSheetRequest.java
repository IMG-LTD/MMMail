package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateSheetsWorkbookSheetRequest(
        @Size(max = 128) String name,
        @Min(1) @Max(200) Integer rowCount,
        @Min(1) @Max(52) Integer colCount
) {
}
