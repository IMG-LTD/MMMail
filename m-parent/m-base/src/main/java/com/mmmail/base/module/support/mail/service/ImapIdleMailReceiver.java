package com.mmmail.base.module.support.mail.service;

import com.mmmail.base.module.support.mail.config.MailProperties;
import com.mmmail.base.module.support.mail.config.MailServerProperties;
import com.mmmail.base.module.support.mail.model.MailUser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Session;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Cascade
 * @description 使用IMAP IDLE命令来实时监听新邮件的服务
 * @date 2025-07-07
 */
@Service
public class ImapIdleMailReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ImapIdleMailReceiver.class);

    private final MailProperties mailProperties;
    private ExecutorService executorService;
    private final java.util.List<ListenerTask> listenerTasks = new java.util.ArrayList<>();
    private final ImapConnectionPool connectionPool;
    private final MailMessageProcessor messageProcessor;
    private final MailCache mailCache;
    private final MessagePushUtil messagePushUtil;
    private final MailServerProperties mailServerProperties;
    private Set<String> lastUserSet = new HashSet<>();

    @Autowired
    public ImapIdleMailReceiver(MailProperties mailProperties, 
                               ImapConnectionPool connectionPool, 
                               MailMessageProcessor messageProcessor,
                               MailCache mailCache,
                               MessagePushUtil messagePushUtil,
                               MailServerProperties mailServerProperties) {
        this.mailProperties = mailProperties;
        this.connectionPool = connectionPool;
        this.messageProcessor = messageProcessor;
        this.mailCache = mailCache;
        this.messagePushUtil = messagePushUtil;
        this.mailServerProperties = mailServerProperties;
    }

    @PostConstruct
    public void startListening() {
        startListeningFromMailCache();
    }

    /**
     * 从 MailCache 启动监听
     */
    private void startListeningFromMailCache() {
        List<MailUser> users = mailCache.getAllUsers();
        Set<String> currentUserSet = users.stream()
            .map(MailUser::getUsername)
            .collect(Collectors.toSet());

        if (currentUserSet.equals(lastUserSet)) {
            logger.debug("MailCache 用户未变动，无需重启IMAP监听。");
            return;
        }

        lastUserSet = currentUserSet;

        if (users.isEmpty()) {
            logger.warn("MailCache 中没有用户信息，IMAP IDLE 监听器将不会启动。");
            return;
        }

        // 停止现有监听任务
        stopListening();

        this.executorService = Executors.newFixedThreadPool(users.size());

        for (MailUser user : users) {
            ListenerTask task = new ListenerTask(user);
            listenerTasks.add(task);
            executorService.submit(task);
        }

        logger.info("已为 {} 个用户启动 IMAP IDLE 监听器", users.size());
    }

    /**
     * 定时刷新监听任务（每5分钟检查一次用户变动）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshListening() {
        logger.info("定时刷新 IMAP IDLE 监听器，检查用户变动...");
        startListeningFromMailCache();
    }
    
    /**
     * 强制重新加载用户并启动监听（用于缓存清理后的恢复）
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 每10分钟检查一次
    public void checkAndReloadUsers() {
        List<MailUser> currentUsers = mailCache.getAllUsers();
        if (currentUsers.isEmpty()) {
            logger.warn("检测到用户缓存为空，请业务模块主动写入用户数据到 MailCache！");
            // TODO: 可在此处发出告警或事件，通知上层业务模块
        } else {
            logger.debug("用户缓存正常，当前有 {} 个用户", currentUsers.size());
        }
    }
    
    // 移除 reloadUsersFromDatabase 方法

    private List<MailProperties.Imap> getAccounts() {
        // 直接从配置文件获取账户信息（保留原有逻辑作为备用）
        if (mailProperties.getMultiAccount().isEnabled()) {
            logger.info("多账户模式已启用，从配置文件加载账户...");
            return mailProperties.getMultiAccount().getAccounts();
        } else {
            logger.info("单账户模式已启用，从配置文件加载账户...");
            return Collections.singletonList(mailProperties.getImap());
        }
    }

    @PreDestroy
    public void stopListening() {
        logger.info("正在停止 IMAP IDLE 监听器...");
        listenerTasks.forEach(ListenerTask::stop);
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("所有 IMAP IDLE 监听器已停止。");
    }

    private class ListenerTask implements Runnable {
        private final MailUser user;
        private Store store;
        private IMAPFolder inbox;
        private volatile boolean running = true;

        public ListenerTask(MailUser user) {
            this.user = user;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    connect();
                    inbox.addMessageCountListener(new MessageCountAdapter() {
                        @Override
                        public void messagesAdded(MessageCountEvent ev) {
                            try {
                                Message[] messages = ev.getMessages();
                                for (Message message : messages) {
                                    logger.info("用户 '{}' 接收到新邮件! 主题: {}, 发件人: {}", user.getUsername(), message.getSubject(), message.getFrom()[0]);
                                    messageProcessor.addMessage(user.getUsername(), message);
                                    
                                    // 推送消息通知
                                    String notificationContent = String.format("你有新邮件: %s", message.getSubject());
                                    messagePushUtil.sendMessage(user.getUsername(), notificationContent);
                                }
                            } catch (MessagingException e) {
                                logger.error("处理新邮件时出错 for user '{}'", user.getUsername(), e);
                            }
                        }
                    });

                    while (running) {
                        try {
                            logger.info("IMAP IDLE 监听器已为用户 '{}' 启动，等待新邮件...", user.getUsername());
                            
                            // 直接尝试 IDLE，如果失败则使用轮询模式
                            try {
                                inbox.idle();
                            } catch (MessagingException e) {
                                String errorMsg = e.getMessage();
                                logger.warn("IDLE 命令失败 for user '{}': {}", user.getUsername(), errorMsg);
                                
                                // 如果是 BAD 错误，说明服务器不支持 IDLE，使用轮询模式
                                if (errorMsg != null && (errorMsg.contains("BAD") || errorMsg.contains("invalid format") || errorMsg.contains("not supported"))) {
                                    logger.info("服务器不支持 IDLE 命令 for user '{}'，切换到轮询模式", user.getUsername());
                                    
                                    // 轮询模式：每30秒检查一次新邮件
                                    while (running) {
                                        try {
                                            Thread.sleep(30000); // 30秒轮询一次
                                            
                                            // 检查是否有新邮件
                                            if (inbox.hasNewMessages()) {
                                                logger.info("轮询模式检测到新邮件 for user: {}", user.getUsername());
                                                // 触发消息处理
                                                inbox.getMessages();
                                            }
                                        } catch (InterruptedException ie) {
                                            Thread.currentThread().interrupt();
                                            running = false;
                                            break;
                                        } catch (MessagingException me) {
                                            logger.error("轮询模式出错 for user '{}'，尝试重新连接", user.getUsername(), me);
                                            reconnect();
                                            break;
                                        }
                                    }
                                } else {
                                    // 其他错误，重新连接
                                    throw e;
                                }
                            }
                        } catch (FolderClosedException e) {
                            logger.warn("文件夹已关闭 for user '{}'，将尝试重新连接...", user.getUsername(), e);
                            reconnect();
                        } catch (MessagingException e) {
                            logger.error("IDLE循环中出现消息传递异常 for user '{}'，将尝试重新连接...", user.getUsername(), e);
                            reconnect();
                        }
                    }
                } catch (Exception e) {
                    logger.error("启动IMAP监听器失败 for user '{}'", user.getUsername(), e);
                    // 等待一段时间后重试，避免快速失败循环
                    try {
                        Thread.sleep(30000); // 30秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        running = false;
                    }
                } finally {
                    closeConnection();
                }
            }
            logger.info("监听任务 for user '{}' 已停止.", user.getUsername());
        }

        private void connect() throws MessagingException {
            Properties props = new Properties();
            
            // 根据端口判断是否使用 SSL
            boolean useSSL = user.getMailPort() == 993 || user.getMailPort() == 465;
            String protocol = useSSL ? "imaps" : "imap";
            
            props.setProperty("mail.store.protocol", protocol);
            
            // 基础配置 - 启用调试以查看详细错误信息
            props.put("mail.debug", "true");
            props.put("mail.debug.auth", "true");
            // 可选：强制只用PLAIN机制
            props.put("mail.imap.auth.mechanisms", "PLAIN");
            
            if (useSSL) {
                // IMAPS 配置
                props.put("mail.imaps.ssl.trust", "*");
                props.put("mail.imaps.ssl.enable", "true");
                props.put("mail.imaps.ssl.protocols", "TLSv1.2");
                props.put("mail.imaps.connectiontimeout", "30000");
                props.put("mail.imaps.timeout", "30000");
                props.put("mail.imaps.readtimeout", "30000");
                props.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.imaps.socketFactory.fallback", "false");
            } else {
                // IMAP + STARTTLS 配置
                props.put("mail.imap.starttls.enable", "true");
                props.put("mail.imap.starttls.required", "true");
                props.put("mail.imap.connectiontimeout", "30000");
                props.put("mail.imap.timeout", "30000");
                props.put("mail.imap.readtimeout", "30000");
                props.put("mail.imap.socketFactory.class", "javax.net.SocketFactory");
            }

            Session session = Session.getInstance(props, null);
            
            try {
                store = session.getStore(protocol);
                logger.info("正在连接 IMAP 服务器: {}:{} (SSL: {}) for user: {}", 
                           user.getMailHost(), user.getMailPort(), useSSL, user.getUsername());
                
                // 只用明文用户名和密码
                String email = user.getEmail();
                store.connect(user.getMailHost(), user.getMailPort(), email, user.getPassword());
                
                logger.info("IMAP 连接成功 for user: {}", user.getUsername());
                
                inbox = (IMAPFolder) store.getFolder("INBOX");
                if (!inbox.exists()) {
                    throw new MessagingException("INBOX 文件夹不存在");
                }
                
                inbox.open(Folder.READ_ONLY);
                logger.info("INBOX 文件夹已打开 for user: {}", user.getUsername());
                
            } catch (MessagingException e) {
                logger.error("IMAP 连接失败 for user: {} - 服务器: {}:{}, 错误: {}", 
                           user.getUsername(), user.getMailHost(), user.getMailPort(), e.getMessage());
                throw e;
            }
        }

        private void closeConnection() {
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(false);
                } catch (MessagingException e) {
                    logger.error("关闭收件箱时出错 for user '{}'", user.getUsername(), e);
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    logger.error("关闭存储时出错 for user '{}'", user.getUsername(), e);
                }
            }
        }

        private void reconnect() {
            closeConnection();
            try {
                Thread.sleep(10000); // 等待10秒再重新连接
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        public void stop() {
            this.running = false;
            if (inbox != null) {
                try {
                    // Interrupt the idle() call
                    if (inbox.isOpen()) {
                        inbox.close(false);
                    }
                } catch (MessagingException e) {
                    logger.warn("中断IDLE时出错 for user '{}'", user.getUsername(), e);
                }
            }
        }
    }
}
