package com.mmmail.base.module.support.mail.service;

import com.mmmail.base.module.support.mail.model.MailUser;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Message;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.alibaba.fastjson.JSON;

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
    public static final String MAIL_USER_DATA_KEY_PREFIX = "mail:user:data:";
    public static final String MAIL_HOST_KEY_PREFIX = "mail:host:";
    public static final String MAIL_PORT_KEY_PREFIX = "mail:port:";
    
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
        redisTemplate.delete(MAIL_HOST_KEY_PREFIX + username);
        redisTemplate.delete(MAIL_PORT_KEY_PREFIX + username);
        redisTemplate.opsForSet().remove(MAIL_LOGIN_USERS_KEY, username);
        // 同时从内存缓存中移除
        userCache.remove(username);
        log.info("用户 {} 的数据已从Redis和内存缓存中删除", username);
    }

    /**
     * 兼容旧方法
     * Compatible with old method
     */
    public void removeUserPwd(String username) {
        removeUserInfo(username);
    }

    /**
     * 从Redis加载用户数据到内存缓存
     * Load user data from Redis to memory cache
     */
    @PostConstruct
    public void loadUsersFromRedis() {
        log.info("从Redis加载用户数据到内存缓存");
        java.util.Set<String> usernames = redisTemplate.opsForSet().members(MAIL_LOGIN_USERS_KEY);
        if (usernames != null && !usernames.isEmpty()) {
            for (String username : usernames) {
                MailUser user = buildUserFromRedis(username);
                if (user != null) {
                    userCache.put(username, user);
                }
            }
            log.info("从Redis加载了 {} 个用户到内存缓存", usernames.size());
        } else {
            log.info("Redis中没有用户数据需要加载");
        }
    }

    /**
     * 从Redis构建用户对象
     * Build user object from Redis data
     */
    private MailUser buildUserFromRedis(String username) {
        String email = redisTemplate.opsForValue().get(MAIL_EMAIL_KEY_PREFIX + username);
        String pwd = redisTemplate.opsForValue().get(MAIL_PWD_KEY_PREFIX + username);
        if (email != null && pwd != null) {
            MailUser user = new MailUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(pwd);
            String mailHost = redisTemplate.opsForValue().get(MAIL_HOST_KEY_PREFIX + username);
            String mailPortStr = redisTemplate.opsForValue().get(MAIL_PORT_KEY_PREFIX + username);
            user.setMailHost(mailHost != null ? mailHost : "mail.mmmail.com"); // 默认值
            user.setMailPort(mailPortStr != null ? Integer.parseInt(mailPortStr) : 993); // 默认值
            return user;
        }
        return null;
    }

    /**
     * 更新或新增用户数据到Redis
     */
    public void saveOrUpdateUserToRedis(MailUser user) {
        if (user != null) {
            redisTemplate.opsForValue().set(MAIL_EMAIL_KEY_PREFIX + user.getUsername(), user.getEmail());
            redisTemplate.opsForValue().set(MAIL_PWD_KEY_PREFIX + user.getUsername(), user.getPassword());
            redisTemplate.opsForValue().set(MAIL_HOST_KEY_PREFIX + user.getUsername(), user.getMailHost());
            redisTemplate.opsForValue().set(MAIL_PORT_KEY_PREFIX + user.getUsername(), String.valueOf(user.getMailPort()));
            redisTemplate.opsForSet().add(MAIL_LOGIN_USERS_KEY, user.getUsername());
            log.info("用户 {} 的数据已在Redis中更新或新增", user.getUsername());
        }
    }

    /**
     * 获取所有已登录用户
     * Get all logged in users
     */
    public java.util.List<MailUser> getAllLoggedInUsers() {
        java.util.Set<String> usernames = redisTemplate.opsForSet().members(MAIL_LOGIN_USERS_KEY);
        if (usernames == null || usernames.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<MailUser> users = new java.util.ArrayList<>();
        for (String username : usernames) {
            MailUser user = buildUserFromRedis(username);
            if (user != null) {
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
    
    /**
     * 用户登录时的处理
     * User login processing
     */
    public void onUserLogin(MailUser user) {
        if (user != null) {
            // 保存到Redis（长期有效）
            saveOrUpdateUserToRedis(user);
            // 同时保存到内存缓存
            userCache.put(user.getUsername(), user);
            log.info("用户 {} 登录，数据已缓存", user.getUsername());
        }
    }
    
    /**
     * 用户更新密码时的处理
     * User password update processing
     */
    public void onUserPasswordUpdate(String username, String newPassword) {
        MailUser user = userCache.get(username);
        if (user != null) {
            user.setPassword(newPassword);
            saveOrUpdateUserToRedis(user);
            userCache.put(username, user);
            log.info("用户 {} 密码已更新", username);
        }
    }
    
    /**
     * 用户重置密码时的处理
     * User password reset processing
     */
    public void onUserPasswordReset(String username, String newPassword) {
        onUserPasswordUpdate(username, newPassword);
        log.info("用户 {} 密码已重置", username);
    }
    
    /**
     * 用户禁用时的处理
     * User disable processing
     */
    public void onUserDisable(String username) {
        removeUserInfo(username);
        log.info("用户 {} 已禁用，数据已清理", username);
    }
    
    /**
     * 用户删除时的处理
     * User delete processing
     */
    public void onUserDelete(String username) {
        removeUserInfo(username);
        log.info("用户 {} 已删除，数据已清理", username);
    }
    
    /**
     * 用户启用时的处理
     * User enable processing
     */
    public void onUserEnable(MailUser user) {
        if (user != null) {
            // 检查Redis中是否已存在
            MailUser existingUser = buildUserFromRedis(user.getUsername());
            if (existingUser != null) {
                // 如果存在，更新数据
                saveOrUpdateUserToRedis(user);
                userCache.put(user.getUsername(), user);
                log.info("用户 {} 启用，数据已更新", user.getUsername());
            } else {
                // 如果不存在，新增数据
                saveOrUpdateUserToRedis(user);
                userCache.put(user.getUsername(), user);
                log.info("用户 {} 启用，数据已新增", user.getUsername());
            }
        }
    }
    
    /**
     * 检查用户是否存在于Redis中
     * Check if user exists in Redis
     */
    public boolean userExistsInRedis(String username) {
        return redisTemplate.opsForSet().isMember(MAIL_LOGIN_USERS_KEY, username);
    }
}
