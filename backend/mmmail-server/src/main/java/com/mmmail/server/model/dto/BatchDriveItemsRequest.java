package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchDriveItemsRequest(
        @NotEmpty List<String> itemIds
) {
}
