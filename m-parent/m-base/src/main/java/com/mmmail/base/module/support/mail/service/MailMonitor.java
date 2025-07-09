package com.mmmail.base.module.support.mail.service;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Data
@Component
public class MailMonitor {
    private static final Logger logger = LoggerFactory.getLogger(MailMonitor.class);
    
    private final ImapConnectionPool connectionPool;
    private final MailMessageProcessor messageProcessor;

    @Autowired
    public MailMonitor(ImapConnectionPool connectionPool, MailMessageProcessor messageProcessor) {
        this.connectionPool = connectionPool;
        this.messageProcessor = messageProcessor;
    }

    public ImapConnectionPool getConnectionPool() {
        return connectionPool;
    }
    public MailMessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    @Scheduled(fixedRate = 300000) // 每30分钟检查一次
    public void monitorResources() {
        int totalConnections = connectionPool.getNumActive() + connectionPool.getNumIdle();
        int activeThreads = messageProcessor.getActiveThreads();
        int queueSize = messageProcessor.getQueueSize();
        int processedMessages = messageProcessor.getProcessedMessages();
        long avgProcessingTime = messageProcessor.getAverageProcessingTime();
        
        // 打印监控信息
        logger.info("邮件系统资源使用报告| Mail System Resource Usage Report:");
        logger.info("- Active Connections: {} / {}", connectionPool.getNumActive(), totalConnections);
        logger.info("- Active Threads: {}", activeThreads);
        logger.info("- Message Queue Size: {}", queueSize);
        logger.info("- Messages Processed: {}", processedMessages);
        logger.info("- Average Processing Time: {} ms", avgProcessingTime);
        
        // 根据监控结果调整系统参数
        try {
        adjustSystemParameters(totalConnections, activeThreads, queueSize);
        } catch (Exception e) {
            logger.warn("自动调整系统参数时发生异常: {}", e.getMessage(), e);
        }
    }

    private void adjustSystemParameters(int totalConnections, int activeThreads, int queueSize) {
        // 如果连接数过高，减少最大连接数
        if (totalConnections > 80) {
            connectionPool.setMaxTotal(totalConnections - 10);
        }
        // 如果队列积压过多，增加处理线程
        if (queueSize > 500) {
            messageProcessor.increaseThreadPoolSize();
        }
        // 如果资源使用率低，减少资源消耗
        if (activeThreads < 10 && queueSize < 100) {
            messageProcessor.reduceThreadPoolSize();
        }
    }
}
