package com.mmmail.server.model.vo;

import java.util.List;

public record AuthenticatorImportResultVo(
        int importedCount,
        int totalCount,
        List<AuthenticatorEntrySummaryVo> entries
) {
}
