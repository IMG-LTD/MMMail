package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestMeetEnterpriseAccessRequest(
        @NotBlank @Size(max = 128) String companyName,
        @Min(1) @Max(50000) Integer requestedSeats,
        @Size(max = 512) String note
) {
}
