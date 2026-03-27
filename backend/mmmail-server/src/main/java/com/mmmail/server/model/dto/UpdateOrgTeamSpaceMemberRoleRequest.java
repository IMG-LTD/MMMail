package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrgTeamSpaceMemberRoleRequest(
        @NotBlank @Pattern(regexp = "MANAGER|EDITOR|VIEWER") String role
) {
}
