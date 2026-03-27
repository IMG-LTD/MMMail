package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MeetQualitySnapshotVo(
        String snapshotId,
        String roomId,
        String participantId,
        int jitterMs,
        int packetLossPercent,
        int roundTripMs,
        int qualityScore,
        LocalDateTime createdAt
) {
}
