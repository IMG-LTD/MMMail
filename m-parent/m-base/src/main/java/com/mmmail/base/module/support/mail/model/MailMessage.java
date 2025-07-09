package com.mmmail.base.module.support.mail.model;

import jakarta.mail.Message;

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

    public String getUsername() {
        return username;
    }

    public Message getMessage() {
        return message;
    }

     public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
