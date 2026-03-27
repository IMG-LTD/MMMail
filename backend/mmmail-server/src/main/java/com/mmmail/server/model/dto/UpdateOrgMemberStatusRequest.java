package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrgMemberStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status
) {
}
