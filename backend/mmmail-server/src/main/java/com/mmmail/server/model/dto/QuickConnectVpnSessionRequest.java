package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record QuickConnectVpnSessionRequest(
        @Size(max = 32) String profileId
) {
}
