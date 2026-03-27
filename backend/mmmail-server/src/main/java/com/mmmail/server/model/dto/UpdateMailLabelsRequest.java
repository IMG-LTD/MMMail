package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateMailLabelsRequest(@Size(max = 20) List<@Size(min = 1, max = 32) String> labels) {
}
