package com.mmmail.base.module.support.mail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MessagePushUtil {
    private final List<MessagePusher> pushers;

    @Autowired
    public MessagePushUtil(List<MessagePusher> pushers) {
        this.pushers = pushers;
    }

    /**
     * 聚合推送消息到所有渠道
     */
    public void sendMessage(String user, String content) {
        for (MessagePusher pusher : pushers) {
            pusher.send(user, content);
        }
    }
} 