package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GeneratePasswordRequest(
        Long orgId,
        @Min(8) @Max(64) Integer length,
        Boolean includeLowercase,
        Boolean includeUppercase,
        Boolean includeDigits,
        Boolean includeSymbols,
        Boolean memorable
) {
}
