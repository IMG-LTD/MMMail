package com.mmmail.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStandardNoteRequest(
        @NotBlank @Size(max = 128) String title,
        String content,
        @Pattern(regexp = "PLAIN_TEXT|MARKDOWN|CHECKLIST") String noteType,
        List<String> tags,
        Long folderId,
        Boolean pinned
) {
}
