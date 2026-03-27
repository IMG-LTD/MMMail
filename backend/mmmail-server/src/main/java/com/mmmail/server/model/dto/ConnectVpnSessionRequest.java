package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnectVpnSessionRequest(
        @NotBlank @Size(max = 64) String serverId,
        @NotBlank @Size(max = 32) String protocol
) {
}
