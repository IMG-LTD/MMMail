package com.mmmail.base.module.support.mail.service;

public interface MessagePusher {
    /**
     * 向指定用户推送消息
     * @param user 用户唯一标识（如邮箱、手机号、IM、TG等）
     * @param content 消息内容
     */
    void send(String user, String content);
} 