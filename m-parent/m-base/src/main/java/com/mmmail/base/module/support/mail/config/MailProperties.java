package com.mmmail.base.module.support.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Cascade
 * @description 用于映射 application.yml 文件中 spring.mail 相关配置的类
 * @date 2025-07-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    private String host;
    private int port;
    private String username;
    private String password;
    private Imap imap = new Imap();
    private MultiAccount multiAccount = new MultiAccount();

    @Data
    public static class Imap {
        private String host;
        private int port;
        private String username;
        private String password;
    }

    @Data
    public static class MultiAccount {
        private boolean enabled = false;
        private java.util.List<Imap> accounts = new java.util.ArrayList<>();
    }
}
