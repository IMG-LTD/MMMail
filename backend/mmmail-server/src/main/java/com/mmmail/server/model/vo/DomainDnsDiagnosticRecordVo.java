package com.mmmail.server.model.vo;

import java.util.List;

public record DomainDnsDiagnosticRecordVo(
        String type,
        String host,
        String expected,
        List<String> actual,
        boolean matched
) {
    public DomainDnsDiagnosticRecordVo {
        actual = List.copyOf(actual);
    }
}
