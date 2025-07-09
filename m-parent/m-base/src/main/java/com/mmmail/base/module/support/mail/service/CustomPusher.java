package com.mmmail.base.module.support.mail.service;

import org.springframework.stereotype.Component;

@Component
public class CustomPusher implements MessagePusher {
    @Override
    public void send(String user, String content) {
        // 示例：可扩展为短信、邮件、企业微信等
        System.out.println("[Custom] 推送给 " + user + ": " + content);
    }
} 