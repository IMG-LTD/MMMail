package com.mmmail.base.module.support.mail.controller;

import com.mmmail.base.module.support.mail.model.MailUser;
import com.mmmail.base.module.support.mail.service.LdapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * SmarterMail对接LDAP接口
 * LDAP controller for SmarterMail integration
 */
@Tag(name = "SmarterMail LDAP接口 | SmarterMail LDAP API")
@RestController
@RequestMapping("/api/ldap")
public class LdapController {

    @Autowired
    private LdapService ldapService;

    /**
     * 用户认证接口
     * Authenticate user
     */
    @Operation(summary = "用户认证 | Authenticate user")
    @PostMapping("/authenticate")
    public boolean authenticate(@RequestParam String username, @RequestParam String password) {
        return ldapService.authenticate(username, password);
    }

    /**
     * 查询用户信息接口
     * Query user info
     */
    @Operation(summary = "查询用户信息 | Query user info")
    @GetMapping("/user")
    public MailUser getUser(@RequestParam String username) {
        return ldapService.getUser(username);
    }

    /**
     * 查询所有用户信息
     * Query all users
     */
    @Operation(summary = "查询所有用户 | Query all users")
    @GetMapping("/users")
    public java.util.List<MailUser> getAllUsers() {
        return ldapService.getAllUsers();
    }

    /**
     * 查询用户是否存在
     * Check if user exists
     */
    @Operation(summary = "用户是否存在 | Check if user exists")
    @GetMapping("/exists")
    public boolean userExists(@RequestParam String username) {
        return ldapService.getUser(username) != null;
    }

    /**
     * 查询所有用户邮箱
     * Query all user emails
     */
    @Operation(summary = "查询所有用户邮箱 | Query all user emails")
    @GetMapping("/emails")
    public java.util.List<String> getAllEmails() {
        return ldapService.getAllEmails();
    }
} 