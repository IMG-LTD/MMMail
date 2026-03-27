package com.mmmail.server.model.vo;

public record DriveVersionCleanupVo(
        int deletedVersions,
        int remainingVersions,
        int appliedRetentionCount,
        int appliedRetentionDays
) {
}
