package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

public record UpdateVpnSettingsRequest(
        @Size(max = 64) String netshieldMode,
        Boolean killSwitchEnabled,
        @Size(max = 32) String defaultConnectionMode,
        @Size(max = 32) String defaultProfileId
) {
}
