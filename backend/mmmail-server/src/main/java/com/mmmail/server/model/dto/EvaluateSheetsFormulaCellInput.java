package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluateSheetsFormulaCellInput(
        @NotBlank @Size(max = 16) String ref,
        @NotBlank @Size(max = 512) String formula
) {
}
