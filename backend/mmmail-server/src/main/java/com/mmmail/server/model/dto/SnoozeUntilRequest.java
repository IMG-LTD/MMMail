package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SnoozeUntilRequest(@NotNull LocalDateTime untilAt) {
}
