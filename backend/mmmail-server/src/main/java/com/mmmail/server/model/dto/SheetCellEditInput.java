package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SheetCellEditInput(
        @NotNull @Min(0) Integer rowIndex,
        @NotNull @Min(0) Integer colIndex,
        String value
) {
}
