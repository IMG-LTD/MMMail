package com.mmmail.base.module.support.mail.service;

import cn.dev33.satoken.stp.StpUtil;
import com.mmmail.base.module.support.mail.model.MailUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SmarterMail对接LDAP服务
 * LDAP service for SmarterMail integration
 */
@Service
public class LdapService {

    @Autowired
    private MailCache mailCache;

    /**
     * 用户认证（用户名+密码）
     * Authenticate user by username and password
     */
    public boolean authenticate(String username, String password) {
        MailUser user = mailCache.getCachedUser(username);
        if (user == null) {
            return false;
        }
        // 这里可根据实际加密方式调整
        boolean match = user.getPassword().equals(password);
        if (match) {
            // sa-token登录
            StpUtil.login(username);
        }
        return match;
    }

    /**
     * 查询用户信息
     * Query user info by username
     */
    public MailUser getUser(String username) {
        return mailCache.getCachedUser(username);
    }

    /**
     * 查询所有用户信息
     */
    public java.util.List<MailUser> getAllUsers() {
        return mailCache.getAllUsers();
    }

    /**
     * 查询所有用户邮箱
     */
    public java.util.List<String> getAllEmails() {
        java.util.List<MailUser> users = mailCache.getAllUsers();
        java.util.List<String> emails = new java.util.ArrayList<>();
        for (MailUser user : users) {
            if (user.getUsername() != null) {
                emails.add(user.getUsername());
            }
        }
        return emails;
    }
} 