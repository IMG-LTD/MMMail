package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrgTeamSpaceMemberRequest(
        @NotBlank @Email @Size(max = 255) String userEmail,
        @NotBlank @Pattern(regexp = "MANAGER|EDITOR|VIEWER") String role
) {
}
