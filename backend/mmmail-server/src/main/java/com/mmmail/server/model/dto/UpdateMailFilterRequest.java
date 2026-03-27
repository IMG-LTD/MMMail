package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateMailFilterRequest(
        @Size(min = 1, max = 64) String name,
        @Size(max = 254) String senderContains,
        @Size(max = 255) String subjectContains,
        @Size(max = 255) String keywordContains,
        @Size(max = 16) String targetFolder,
        Long targetCustomFolderId,
        @Size(max = 20) List<@Size(min = 1, max = 32) String> labels,
        Boolean markRead,
        Boolean enabled
) {
}
