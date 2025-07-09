package com.mmmail.base.module.support.mail.controller;

import com.mmmail.base.module.support.mail.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/test")
    public String sendTestEmail() {
        try {
            emailService.sendSimpleMessage("1229426238@qq.com", "Test Email", "This is a test email from MMMail!");
            return "Email sent successfully!";
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }
}
