package com.mmmail.base.module.support.mail.service;

import org.springframework.stereotype.Component;

@Component
public class TgBotPusher implements MessagePusher {
    @Override
    public void send(String user, String content) {
        // 示例：实际应调用TG机器人API
        // user 可为TG用户id或群id
        System.out.println("[TG] 推送给 " + user + ": " + content);
    }
} 