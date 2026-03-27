package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record PassSharedVaultMemberVo(
        String id,
        String userId,
        String userEmail,
        String role,
        LocalDateTime updatedAt
) {
}
