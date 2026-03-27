package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;

public record MailActionRequest(@NotBlank String action) {
}
