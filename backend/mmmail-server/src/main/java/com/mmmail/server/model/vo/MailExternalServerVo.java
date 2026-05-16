package com.mmmail.server.model.vo;

public record MailExternalServerVo(
        String host,
        Integer port,
        boolean ssl,
        boolean starttls
) {
}
