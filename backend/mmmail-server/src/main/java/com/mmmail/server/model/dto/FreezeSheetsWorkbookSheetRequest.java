package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FreezeSheetsWorkbookSheetRequest(
        @NotNull @Min(1) Integer currentVersion,
        @NotNull @Min(0) Integer frozenRowCount,
        @NotNull @Min(0) Integer frozenColCount
) {
}
