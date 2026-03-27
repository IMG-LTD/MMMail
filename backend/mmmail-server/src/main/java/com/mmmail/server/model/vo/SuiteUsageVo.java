package com.mmmail.server.model.vo;

public record SuiteUsageVo(
        long mailCount,
        long contactCount,
        long calendarEventCount,
        long calendarShareCount,
        long driveFileCount,
        long driveFolderCount,
        long driveStorageBytes
) {
}
