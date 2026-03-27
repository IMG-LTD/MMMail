package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateSheetsWorkbookCellsRequest(
        @NotNull @Min(1) Integer currentVersion,
        @Size(max = 64) String sheetId,
        @NotEmpty List<@Valid SheetCellEditInput> edits
) {
}
