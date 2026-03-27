package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateLumoConversationArchiveRequest(
        @NotNull Boolean archived
) {
}
