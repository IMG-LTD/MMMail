package com.mmmail.server.model.vo;

import java.util.List;

public record DomainDnsRecordsVo(
        List<DomainDnsRecordVo> records
) {
    public DomainDnsRecordsVo {
        records = List.copyOf(records);
    }
}
