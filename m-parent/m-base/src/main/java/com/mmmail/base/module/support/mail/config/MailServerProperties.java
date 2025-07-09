package com.mmmail.base.module.support.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件服务器配置（用于 MailUser 统一管理）
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail-server")
public class MailServerProperties {
    private String host = "mail.mmmail.com";
    private int port = 587;
    private String emailSuffix = "@mmmail.com";
} 