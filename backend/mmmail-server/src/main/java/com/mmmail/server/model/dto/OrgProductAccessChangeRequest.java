package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrgProductAccessChangeRequest(
        @NotBlank @Pattern(regexp = "MAIL|CALENDAR|DRIVE|DOCS|SHEETS|PASS|SIMPLELOGIN|STANDARD_NOTES|VPN|WALLET|AUTHENTICATOR|MEET|LUMO") String productKey,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED") String accessState
) {
}
