package com.mmmail.server.model.vo;

public record ContactImportResultVo(
        int totalRows,
        int created,
        int updated,
        int skipped,
        int invalid
) {
}
