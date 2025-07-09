package com.mmmail.base.module.support.mail.service;

import org.springframework.stereotype.Component;

@Component
public class DingTalkPusher implements MessagePusher {
    @Override
    public void send(String user, String content) {
        // 示例：实际应调用钉钉机器人API
        // user 可为钉钉群机器人webhook或用户id
        System.out.println("[DingTalk] 推送给 " + user + ": " + content);
    }
} 