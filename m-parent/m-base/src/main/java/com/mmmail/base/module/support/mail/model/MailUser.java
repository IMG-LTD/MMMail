package com.mmmail.base.module.support.mail.model;

import lombok.Data;

@Data
public class MailUser {
    private String username;
    private String email;
    private String password;
    private String mailHost;
    private int mailPort;
}
