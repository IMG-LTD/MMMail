package com.mmmail.server.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EvaluateSheetsCellsRequest(
        @NotEmpty List<@Valid EvaluateSheetsFormulaCellInput> cells
) {
}
