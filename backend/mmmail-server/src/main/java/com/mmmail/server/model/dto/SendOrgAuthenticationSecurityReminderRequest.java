package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SendOrgAuthenticationSecurityReminderRequest(
        @NotEmpty
        @Size(max = 50)
        List<Long> memberIds
) {
}
