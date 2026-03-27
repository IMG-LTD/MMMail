package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVpnProfileRequest(
        @NotBlank @Size(min = 2, max = 64) String name,
        @NotBlank @Size(max = 32) String protocol,
        @NotBlank @Size(max = 32) String routingMode,
        @Size(max = 64) String targetServerId,
        @Size(max = 64) String targetCountry,
        Boolean secureCoreEnabled,
        @Size(max = 64) String netshieldMode,
        Boolean killSwitchEnabled
) {
}
