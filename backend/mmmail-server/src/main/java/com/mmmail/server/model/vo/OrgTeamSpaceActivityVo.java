package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record OrgTeamSpaceActivityVo(
        String id,
        String teamSpaceId,
        String actorEmail,
        String eventType,
        String detail,
        LocalDateTime createdAt
) {
}
