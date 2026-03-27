package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SortSheetsWorkbookSheetRequest(
        @NotNull @Min(1) Integer currentVersion,
        @NotNull @Min(0) @Max(51) Integer columnIndex,
        @NotNull @Pattern(regexp = "ASC|DESC") String direction,
        @NotNull Boolean includeHeader
) {
}
