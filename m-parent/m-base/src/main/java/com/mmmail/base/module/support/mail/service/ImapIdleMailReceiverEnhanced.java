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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Enhanced Version
 * @description 增强版IMAP IDLE邮件监听服务，提供详细日志记录和完整业务逻辑覆盖
 * @date 2025-07-11
 */
@Service
public class ImapIdleMailReceiverEnhanced {

    private static final Logger logger = LoggerFactory.getLogger(ImapIdleMailReceiverEnhanced.class);
    
    // 监听器状态统计
    private final AtomicInteger totalTaskCount = new AtomicInteger(0);
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicInteger failedTaskCount = new AtomicInteger(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalConnectionAttempts = new AtomicLong(0);
    private final AtomicLong totalConnectionFailures = new AtomicLong(0);
    private final Map<String, TaskStatistics> taskStatistics = new ConcurrentHashMap<>();
    
    private final MailProperties mailProperties;
    private ExecutorService executorService;
    private final List<ListenerTask> listenerTasks = new CopyOnWriteArrayList<>();
    private final ImapConnectionPool connectionPool;
    private final MailMessageProcessor messageProcessor;
    private final MailCache mailCache;
    private final MessagePushUtil messagePushUtil;
    private final MailServerProperties mailServerProperties;
    private Set<String> lastUserSet = new HashSet<>();
    private volatile boolean serviceRunning = false;
    private final Object serviceLock = new Object();

    @Autowired
    public ImapIdleMailReceiverEnhanced(MailProperties mailProperties,
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
        logger.info("===== ImapIdleMailReceiver 服务初始化开始 =====");
        synchronized (serviceLock) {
            if (!serviceRunning) {
                serviceRunning = true;
                startListeningFromMailCache();
                logger.info("===== ImapIdleMailReceiver 服务初始化完成 =====");
            }
        }
    }

    /**
     * 从 MailCache 启动监听
     */
    private void startListeningFromMailCache() {
        logger.info("开始从 MailCache 加载用户并启动监听...");
        
        List<MailUser> users = mailCache.getAllUsers();
        Set<String> currentUserSet = users.stream()
            .map(MailUser::getUsername)
            .collect(Collectors.toSet());

        logger.info("当前用户集合: {}, 上次用户集合: {}", currentUserSet, lastUserSet);
        
        if (currentUserSet.equals(lastUserSet)) {
            logger.debug("MailCache 用户未变动，跳过重启IMAP监听");
            return;
        }

        // 记录用户变动详情
        Set<String> addedUsers = new HashSet<>(currentUserSet);
        addedUsers.removeAll(lastUserSet);
        Set<String> removedUsers = new HashSet<>(lastUserSet);
        removedUsers.removeAll(currentUserSet);
        
        if (!addedUsers.isEmpty()) {
            logger.info("检测到新增用户: {}", addedUsers);
        }
        if (!removedUsers.isEmpty()) {
            logger.info("检测到移除用户: {}", removedUsers);
        }

        lastUserSet = currentUserSet;

        if (users.isEmpty()) {
            logger.warn("⚠️ MailCache 中没有用户信息，IMAP IDLE 监听器将不会启动");
            return;
        }

        // 停止现有监听任务
        stopListening();

        // 创建新的线程池
        this.executorService = new ThreadPoolExecutor(
            users.size(), 
            users.size() * 2, 
            60L, 
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("ImapListener-" + totalTaskCount.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        );

        logger.info("创建线程池: 核心线程数={}, 最大线程数={}", users.size(), users.size() * 2);

        // 为每个用户创建监听任务
        for (MailUser user : users) {
            try {
                ListenerTask task = new ListenerTask(user);
                listenerTasks.add(task);
                executorService.submit(task);
                logger.info("✅ 为用户 '{}' 创建监听任务", user.getUsername());
            } catch (Exception e) {
                logger.error("❌ 为用户 '{}' 创建监听任务失败", user.getUsername(), e);
                failedTaskCount.incrementAndGet();
            }
        }

        logger.info("🚀 已为 {} 个用户启动 IMAP IDLE 监听器", users.size());
        logServiceStatistics();
    }

    /**
     * 定时刷新监听任务（每5分钟检查一次用户变动）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshListening() {
        logger.info("⏰ 定时刷新 IMAP IDLE 监听器，检查用户变动...");
        if (serviceRunning) {
            startListeningFromMailCache();
        }
    }

    /**
     * 检查服务健康状态（每10分钟检查一次）
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void checkServiceHealth() {
        logger.info("🔍 检查服务健康状态...");
        
        List<MailUser> currentUsers = mailCache.getAllUsers();
        if (currentUsers.isEmpty()) {
            logger.warn("⚠️ 检测到用户缓存为空，请业务模块主动写入用户数据到 MailCache！");
        } else {
            logger.info("✅ 用户缓存正常，当前有 {} 个用户", currentUsers.size());
        }
        
        // 检查线程池状态
        if (executorService != null) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;
            logger.info("📊 线程池状态 - 活跃线程: {}, 任务队列: {}, 已完成任务: {}", 
                executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount());
        }
        
        // 检查监听任务状态
        int runningTasks = 0;
        int stoppedTasks = 0;
        for (ListenerTask task : listenerTasks) {
            if (task.isRunning()) {
                runningTasks++;
            } else {
                stoppedTasks++;
            }
        }
        
        logger.info("📊 监听任务状态 - 运行中: {}, 已停止: {}", runningTasks, stoppedTasks);
        
        // 记录统计信息
        logServiceStatistics();
    }

    /**
     * 记录服务统计信息
     */
    private void logServiceStatistics() {
        logger.info("📊 服务统计信息:");
        logger.info("   - 总任务数: {}", totalTaskCount.get());
        logger.info("   - 活跃任务数: {}", activeTaskCount.get());
        logger.info("   - 失败任务数: {}", failedTaskCount.get());
        logger.info("   - 总接收邮件数: {}", totalMessagesReceived.get());
        logger.info("   - 总连接尝试数: {}", totalConnectionAttempts.get());
        logger.info("   - 总连接失败数: {}", totalConnectionFailures.get());
        
        // 记录每个用户的统计信息
        taskStatistics.forEach((username, stats) -> {
            logger.info("   - 用户 '{}': 接收邮件 {}, 连接尝试 {}, 连接失败 {}, 运行时间 {}",
                username, stats.messageCount.get(), stats.connectionAttempts.get(), 
                stats.connectionFailures.get(), stats.getRunningTime());
        });
    }

    @PreDestroy
    public void stopListening() {
        logger.info("🛑 正在停止 IMAP IDLE 监听器...");
        
        synchronized (serviceLock) {
            serviceRunning = false;
            
            // 停止所有监听任务
            for (ListenerTask task : listenerTasks) {
                try {
                    task.stop();
                } catch (Exception e) {
                    logger.error("停止监听任务时出错", e);
                }
            }
            listenerTasks.clear();
            
            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.warn("线程池未能在60秒内正常关闭，强制关闭");
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.warn("等待线程池关闭时被中断");
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // 清理统计信息
            taskStatistics.clear();
            
            logger.info("✅ 所有 IMAP IDLE 监听器已停止");
        }
    }

    /**
     * 任务统计信息类
     */
    private static class TaskStatistics {
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private final AtomicInteger connectionAttempts = new AtomicInteger(0);
        private final AtomicInteger connectionFailures = new AtomicInteger(0);
        private final LocalDateTime startTime = LocalDateTime.now();
        
        public String getRunningTime() {
            return java.time.Duration.between(startTime, LocalDateTime.now()).toString();
        }
    }

    /**
     * 增强的监听任务类
     */
    private class ListenerTask implements Runnable {
        private final MailUser user;
        private Store store;
        private IMAPFolder inbox;
        private volatile boolean running = true;
        private final String taskStartTime;
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private final AtomicLong totalProcessingTime = new AtomicLong(0);
        private final AtomicReference<String> currentStatus = new AtomicReference<>("Initializing");
        private final TaskStatistics statistics;

        public ListenerTask(MailUser user) {
            this.user = user;
            this.taskStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.statistics = new TaskStatistics();
            taskStatistics.put(user.getUsername(), statistics);
            activeTaskCount.incrementAndGet();
        }

        @Override
        public void run() {
            logger.info("🏃 用户 '{}' 的监听任务开始运行，启动时间: {}", user.getUsername(), taskStartTime);
            
            while (running) {
                try {
                    connect();
                    setupMessageListener();
                    startIdleLoop();
                } catch (Exception e) {
                    logger.error("❌ 用户 '{}' 的监听任务运行异常", user.getUsername(), e);
                    handleTaskFailure();
                } finally {
                    closeConnection();
                }
            }
            
            activeTaskCount.decrementAndGet();
            logger.info("🔚 用户 '{}' 的监听任务已停止，总运行时间: {}", user.getUsername(), statistics.getRunningTime());
        }

        private void connect() throws MessagingException {
            currentStatus.set("Connecting");
            statistics.connectionAttempts.incrementAndGet();
            totalConnectionAttempts.incrementAndGet();
            
            Properties props = new Properties();
            
            // 根据端口判断是否使用 SSL
            boolean useSSL = user.getMailPort() == 993 || user.getMailPort() == 465;
            String protocol = useSSL ? "imaps" : "imap";
            
            props.setProperty("mail.store.protocol", protocol);
            
            // 基础配置
            props.put("mail.debug", "false"); // 生产环境关闭调试
            props.put("mail.debug.auth", "false");
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
                logger.info("🔌 正在连接 IMAP 服务器: {}:{} (SSL: {}) for user: {}", 
                           user.getMailHost(), user.getMailPort(), useSSL, user.getUsername());
                
                String email = user.getEmail();
                store.connect(user.getMailHost(), user.getMailPort(), email, user.getPassword());
                
                currentStatus.set("Connected");
                logger.info("✅ IMAP 连接成功 for user: {}", user.getUsername());
                
                inbox = (IMAPFolder) store.getFolder("INBOX");
                if (!inbox.exists()) {
                    throw new MessagingException("INBOX 文件夹不存在");
                }
                
                inbox.open(Folder.READ_ONLY);
                currentStatus.set("Ready");
                logger.info("📫 INBOX 文件夹已打开 for user: {}", user.getUsername());
                
            } catch (MessagingException e) {
                statistics.connectionFailures.incrementAndGet();
                totalConnectionFailures.incrementAndGet();
                currentStatus.set("Connection Failed");
                logger.error("❌ IMAP 连接失败 for user: {} - 服务器: {}:{}, 错误: {}", 
                           user.getUsername(), user.getMailHost(), user.getMailPort(), e.getMessage());
                throw e;
            }
        }

        private void setupMessageListener() {
            inbox.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent ev) {
                    long startTime = System.nanoTime();
                    try {
                        Message[] messages = ev.getMessages();
                        logger.info("📧 用户 '{}' 接收到 {} 封新邮件", user.getUsername(), messages.length);
                        
                        for (Message message : messages) {
                            processNewMessage(message);
                        }
                        
                        long endTime = System.nanoTime();
                        long processingTime = (endTime - startTime) / 1_000_000; // milliseconds
                        totalProcessingTime.addAndGet(processingTime);
                        
                        logger.info("📈 用户 '{}' 邮件处理完成，耗时: {} ms", user.getUsername(), processingTime);
                        
                    } catch (Exception e) {
                        logger.error("❌ 处理新邮件时出错 for user '{}'", user.getUsername(), e);
                    }
                }
            });
        }

        private void processNewMessage(Message message) throws MessagingException {
            String subject = message.getSubject();
            String from = (message.getFrom() != null && message.getFrom().length > 0) 
                ? message.getFrom()[0].toString() : "未知发件人";
            
            logger.info("📬 用户 '{}' 接收到新邮件! 主题: '{}', 发件人: '{}'", 
                user.getUsername(), subject, from);
            
            // 处理邮件
            messageProcessor.addMessage(user.getUsername(), message);
            messageCount.incrementAndGet();
            statistics.messageCount.incrementAndGet();
            totalMessagesReceived.incrementAndGet();
            
            // 推送消息通知
            String notificationContent = String.format("你有新邮件: %s", subject);
            try {
                messagePushUtil.sendMessage(user.getUsername(), notificationContent);
                logger.info("📱 用户 '{}' 邮件通知推送成功", user.getUsername());
            } catch (Exception e) {
                logger.error("❌ 用户 '{}' 邮件通知推送失败", user.getUsername(), e);
            }
        }

        private void startIdleLoop() {
            while (running) {
                try {
                    currentStatus.set("Listening");
                    logger.info("👂 用户 '{}' IMAP IDLE 监听已启动，等待新邮件...", user.getUsername());
                    
                    // 尝试 IDLE 模式
                    inbox.idle();
                    logger.debug("💤 用户 '{}' IDLE 模式正常唤醒", user.getUsername());

                } catch (FolderClosedException e) {
                    logger.warn("📁 用户 '{}' 文件夹已关闭，将尝试重新连接", user.getUsername());
                    reconnect();
                } catch (MessagingException e) {
                    logger.error("❌ 用户 '{}' IDLE循环中出现异常，将尝试重新连接", user.getUsername(), e);
                    reconnect();
                } catch (Exception e) {
                    logger.error("❌ 用户 '{}' 发生未知异常", user.getUsername(), e);
                    break;
                }
            }
        }

        private void handleIdleFailure(MessagingException e) {
            String errorMsg = e.getMessage();
            logger.warn("⚠️ 用户 '{}' IDLE 命令失败: {}", user.getUsername(), errorMsg);
            
            // 如果服务器不支持 IDLE，使用轮询模式
            if (errorMsg != null && (errorMsg.contains("BAD") || errorMsg.contains("invalid format") || errorMsg.contains("not supported"))) {
                logger.info("🔄 用户 '{}' 服务器不支持 IDLE 命令，切换到轮询模式", user.getUsername());
                startPollingMode();
            } else {
                throw new RuntimeException("IDLE 命令失败", e);
            }
        }

        private void startPollingMode() {
            currentStatus.set("Polling");
            logger.info("🔁 用户 '{}' 开始轮询模式，每30秒检查一次", user.getUsername());
            
            while (running) {
                try {
                    Thread.sleep(30000); // 30秒轮询一次
                    
                    if (inbox.hasNewMessages()) {
                        logger.info("📧 用户 '{}' 轮询模式检测到新邮件", user.getUsername());
                        // 触发消息处理
                        inbox.getMessages();
                    }
                    
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                } catch (MessagingException me) {
                    logger.error("❌ 用户 '{}' 轮询模式出错，尝试重新连接", user.getUsername(), me);
                    reconnect();
                    break;
                }
            }
        }

        private void handleTaskFailure() {
            failedTaskCount.incrementAndGet();
            currentStatus.set("Failed");
            
            // 等待一段时间后重试，避免快速失败循环
            try {
                logger.info("⏳ 用户 '{}' 任务失败，30秒后重试", user.getUsername());
                Thread.sleep(30000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        private void closeConnection() {
            currentStatus.set("Disconnecting");
            
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(false);
                    logger.debug("📫 用户 '{}' INBOX 已关闭", user.getUsername());
                } catch (MessagingException e) {
                    logger.error("❌ 用户 '{}' 关闭收件箱时出错", user.getUsername(), e);
                }
            }
            
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                    logger.debug("🔌 用户 '{}' 存储连接已关闭", user.getUsername());
                } catch (MessagingException e) {
                    logger.error("❌ 用户 '{}' 关闭存储连接时出错", user.getUsername(), e);
                }
            }
            
            currentStatus.set("Disconnected");
        }

        private void reconnect() {
            currentStatus.set("Reconnecting");
            closeConnection();
            
            try {
                logger.info("🔄 用户 '{}' 等待10秒后重新连接", user.getUsername());
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        public void stop() {
            this.running = false;
            currentStatus.set("Stopping");
            
            if (inbox != null) {
                try {
                    if (inbox.isOpen()) {
                        inbox.close(false);
                    }
                } catch (MessagingException e) {
                    logger.warn("⚠️ 用户 '{}' 中断IDLE时出错", user.getUsername(), e);
                }
            }
            
            logger.info("🛑 用户 '{}' 监听任务已停止", user.getUsername());
        }

        public boolean isRunning() {
            return running;
        }

        public String getCurrentStatus() {
            return currentStatus.get();
        }

        public int getMessageCount() {
            return messageCount.get();
        }

        public long getTotalProcessingTime() {
            return totalProcessingTime.get();
        }
    }
}
