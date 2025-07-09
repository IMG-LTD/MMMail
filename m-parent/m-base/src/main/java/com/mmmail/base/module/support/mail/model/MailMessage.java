package com.mmmail.base.module.support.mail.model;

import jakarta.mail.Message;
import lombok.Data;

@Data
public class MailMessage {
    private final String username;
    private final Message message;
    private boolean processed;
    private long processingTime;
    private String errorMessage;

    public MailMessage(String username, Message message) {
        this.username = username;
        this.message = message;
        this.processed = false;
        this.processingTime = 0;
        this.errorMessage = null;
    }
}
