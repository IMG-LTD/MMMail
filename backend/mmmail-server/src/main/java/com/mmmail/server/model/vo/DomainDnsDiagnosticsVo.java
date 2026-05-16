package com.mmmail.server.model.vo;

import java.util.List;

public record DomainDnsDiagnosticsVo(
        String domainId,
        String domain,
        String status,
        List<DomainDnsDiagnosticRecordVo> records
) {
    public DomainDnsDiagnosticsVo {
        records = List.copyOf(records);
    }
}
