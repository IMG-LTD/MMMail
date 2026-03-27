package com.mmmail.server.model.vo;

public record DriveFilePreviewVo(
        String fileName,
        String mimeType,
        byte[] content,
        boolean truncated
) {
}
