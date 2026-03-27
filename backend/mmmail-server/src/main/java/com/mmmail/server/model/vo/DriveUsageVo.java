package com.mmmail.server.model.vo;

public record DriveUsageVo(
        long fileCount,
        long folderCount,
        long storageBytes,
        long storageLimitBytes
) {
}
