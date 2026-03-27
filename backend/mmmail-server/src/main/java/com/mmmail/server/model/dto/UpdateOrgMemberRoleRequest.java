package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrgMemberRoleRequest(
        @NotBlank @Pattern(regexp = "ADMIN|MEMBER") String role
) {
}
