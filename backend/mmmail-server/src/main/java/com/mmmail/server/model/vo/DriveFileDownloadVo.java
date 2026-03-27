package com.mmmail.server.model.vo;

public record DriveFileDownloadVo(
        String fileName,
        String mimeType,
        byte[] content
) {
}
