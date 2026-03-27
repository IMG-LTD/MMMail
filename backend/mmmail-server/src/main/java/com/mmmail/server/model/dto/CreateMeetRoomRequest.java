package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMeetRoomRequest(
        @NotBlank @Size(min = 3, max = 128) String topic,
        @Pattern(regexp = "PRIVATE|PUBLIC", message = "accessLevel must be PRIVATE or PUBLIC")
        String accessLevel,
        @Min(2) @Max(200) Integer maxParticipants
) {
}
