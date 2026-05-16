package com.mmmail.server.model.vo;

public record DomainDnsRecordVo(
        String type,
        String host,
        String expected
) {
}
