package com.mmmail.base.module.support.mail.service;

import com.mmmail.base.module.support.mail.model.MailUser;
import jakarta.mail.Message;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MailCache {
    // 缓存压缩后的邮件字节数组
    private final Map<String, byte[]> messageCache = new ConcurrentHashMap<>();
    private final Map<String, MailUser> userCache = new ConcurrentHashMap<>();
    
    private static final long CACHE_TTL = TimeUnit.HOURS.toMillis(1); // 1小时过期
    private static final Logger log = LoggerFactory.getLogger(MailCache.class);
    @Autowired
    private MessageCompressor messageCompressor;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public static final String MAIL_PWD_KEY_PREFIX = "mail:pwd:";
    public static final String MAIL_EMAIL_KEY_PREFIX = "mail:email:";
    public static final String MAIL_LOGIN_USERS_KEY = "mail:login:users";
    
    /**
     * 缓存邮件消息
     * Cache mail message
     */
    public void cacheMessage(String key, Message message) {
        try {
            byte[] compressed = messageCompressor.compressMessage(message);
            messageCache.put(key, compressed);
            log.info("缓存压缩后的邮件消息 | Cache compressed mail message");
        } catch (Exception e) {
            log.error("缓存邮件消息失败 | Failed to cache mail message: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取缓存的邮件消息
     * Get cached mail message
     */
    public Message getCachedMessage(String key) {
        try {
            byte[] compressed = messageCache.get(key);
            if (compressed == null) return null;
            // 这里假设Session为null，实际应从业务上下文传入
            // 可通过Spring注入或参数传递
            return messageCompressor.decompressMessage(compressed, null);
        } catch (Exception e) {
            System.out.println("获取缓存邮件消息失败 | Failed to get cached mail message: " + e.getMessage());
            return null;
        }
    }

    /**
     * 缓存用户信息
     * Cache user info
     */
    public void cacheUser(String key, MailUser user) {
        userCache.put(key, user);
        log.info("缓存用户信息 | Cache user info");
    }

    /**
     * 批量写入用户缓存
     * Batch cache user info
     */
    public void cacheUsers(Map<String, MailUser> users) {
        if (users != null && !users.isEmpty()) {
            userCache.putAll(users);
            log.info("批量写入用户缓存 | Batch cache user info, size: {}", users.size());
        }
    }

    /**
     * 批量移除用户缓存
     * Batch remove user cache
     */
    public void removeUsers(Iterable<String> usernames) {
        if (usernames != null) {
            for (String username : usernames) {
                userCache.remove(username);
            }
            log.info("批量移除用户缓存 | Batch remove user cache");
        }
    }

    /**
     * 获取缓存的用户信息
     * Get cached user info
     */
    public MailUser getCachedUser(String key) {
        return userCache.get(key);
    }

    /**
     * 从缓存中获取用户（带Spring Cache注解）
     * Get user from cache (with Spring Cache annotation)
     */
    @Cacheable(value = "mailUsers", key = "#username", unless = "#result == null")
    public MailUser getUserFromCache(String username) {
        return userCache.get(username);
    }

    /**
     * 移除指定用户缓存
     * Remove cached user by username
     */
    public void removeUser(String username) {
        userCache.remove(username);
        log.info("移除用户缓存 | Remove cached user: {}", username);
    }

    /**
     * 获取所有缓存用户
     * Get all cached users
     */
    public java.util.List<MailUser> getAllUsers() {
        return new java.util.ArrayList<>(userCache.values());
    }

    /**
     * 登录时写入用户密码
     * Cache user password on login
     */
    public void cacheUserInfo(String username, String email, String pwd) {
        redisTemplate.opsForValue().set(MAIL_PWD_KEY_PREFIX + username, pwd);
        redisTemplate.opsForValue().set(MAIL_EMAIL_KEY_PREFIX + username, email);
        redisTemplate.opsForSet().add(MAIL_LOGIN_USERS_KEY, username);
    }

    /**
     * 兼容旧方法
     * Compatible with old method
     */
    public void cacheUserPwd(String username, String pwd) {
        String email = redisTemplate.opsForValue().get(MAIL_EMAIL_KEY_PREFIX + username);
        if (email == null) email = username; // fallback
        cacheUserInfo(username, email, pwd);
    }

    /**
     * 退出时删除用户密码
     * Remove user password on logout
     */
    public void removeUserInfo(String username) {
        redisTemplate.delete(MAIL_PWD_KEY_PREFIX + username);
        redisTemplate.delete(MAIL_EMAIL_KEY_PREFIX + username);
        redisTemplate.opsForSet().remove(MAIL_LOGIN_USERS_KEY, username);
    }

    /**
     * 兼容旧方法
     * Compatible with old method
     */
    public void removeUserPwd(String username) {
        removeUserInfo(username);
    }

    /**
     * 获取所有已登录用户
     * Get all logged-in users
     */
    public java.util.List<MailUser> getAllLoggedInUsers() {
        java.util.Set<String> usernames = redisTemplate.opsForSet().members(MAIL_LOGIN_USERS_KEY);
        if (usernames == null || usernames.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<MailUser> users = new java.util.ArrayList<>();
        for (String username : usernames) {
            String pwd = redisTemplate.opsForValue().get(MAIL_PWD_KEY_PREFIX + username);
            String email = redisTemplate.opsForValue().get(MAIL_EMAIL_KEY_PREFIX + username);
            if (pwd != null && email != null) {
                MailUser user = new MailUser();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(pwd);
                users.add(user);
            }
        }
        return users;
    }

    /**
     * 定时清理缓存（每小时执行一次）
     * Scheduled cache cleanup (every hour)
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void scheduledCleanup() {
        log.info("启动缓存清理线程 | Starting cache cleanup thread");
        new Thread(() -> {
            try {
                Thread.sleep(CACHE_TTL);
                log.info("缓存清理线程触发 | Cache cleanup thread triggered");
                cleanupMessageCacheOnly(); // 只清理消息缓存，保留用户缓存
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("缓存清理线程中断 | Cache cleanup thread interrupted", e);
            }
        }).start();
        log.info("缓存清理线程已启动 | Cache cleanup thread started");
    }

    /**
     * 清理缓存
     * Cleanup cache
     */
    private void cleanupCache() {
        log.info("开始清理缓存 | Starting cache cleanup");
        messageCache.clear();
        // 不清理用户缓存，避免影响邮件监听服务
        // userCache.clear();
        log.info("缓存已清理（保留用户缓存） | Cache has been cleaned (user cache preserved)");
    }
    
    /**
     * 只清理消息缓存，保留用户缓存
     * Cleanup message cache only, preserve user cache
     */
    private void cleanupMessageCacheOnly() {
        log.info("开始清理消息缓存 | Starting message cache cleanup");
        int messageCount = messageCache.size();
        messageCache.clear();
        log.info("消息缓存已清理，清理了 {} 条消息 | Message cache cleaned, removed {} messages", messageCount, messageCount);
    }

    /**
     * 清空所有缓存
     * Clear all cache
     */
    public void clearCache() {
        messageCache.clear();
        userCache.clear();
        log.info("清空所有缓存 | Clear all cache");
    }

    /**
     * 获取消息缓存大小
     * Get message cache size
     */
    public int getMessageCacheSize() {
        return messageCache.size();
    }

    /**
     * 获取用户缓存大小
     * Get user cache size
     */
    public int getUserCacheSize() {
        return userCache.size();
    }
    
    /**
     * 手动清理消息缓存（保留用户缓存）
     * Manual cleanup message cache (preserve user cache)
     */
    public void clearMessageCache() {
        cleanupMessageCacheOnly();
    }
}
