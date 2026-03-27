package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReportMeetQualityRequest(
        @NotNull @Min(0) @Max(5000) Integer jitterMs,
        @NotNull @Min(0) @Max(100) Integer packetLossPercent,
        @NotNull @Min(0) @Max(10000) Integer roundTripMs
) {
}
