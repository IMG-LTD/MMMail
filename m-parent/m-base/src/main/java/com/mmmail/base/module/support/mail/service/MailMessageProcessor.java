package com.mmmail.base.module.support.mail.service;

import com.mmmail.base.module.support.mail.model.MailMessage;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MailMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MailMessageProcessor.class);
    private final BlockingQueue<MailMessage> messageQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor threadPool;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger processingMessages = new AtomicInteger(0);
    private final AtomicInteger totalMessages = new AtomicInteger(0);
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicInteger failedMessages = new AtomicInteger(0);
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000;  // 5秒重试间隔
    private final AtomicInteger retryMessages = new AtomicInteger(0);
    private final AtomicLong totalRetryTime = new AtomicLong(0);
    
    // 消息处理时间统计
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong maxProcessingTime = new AtomicLong(0);
    private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
    private volatile boolean running = true;

    private final AtomicInteger currentCorePoolSize;
    private final AtomicInteger currentMaximumPoolSize;

    private final Thread processorThread;

    public MailMessageProcessor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize * 2;
        currentCorePoolSize = new AtomicInteger(corePoolSize);
        currentMaximumPoolSize = new AtomicInteger(maximumPoolSize);
        threadPool = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        threadPool.setRejectedExecutionHandler((r, executor) -> {
            if (!executor.isShutdown()) {
                logger.warn("线程池已满，使用调用者线程执行任务");
                r.run();
            }
        });
        processorThread = new Thread(this::processMessages);
        processorThread.setName("MailMessageProcessor");
        processorThread.start();
    }

    public void increaseThreadPoolSize() {
        int newCorePoolSize = currentCorePoolSize.incrementAndGet();
        int newMaximumPoolSize = currentMaximumPoolSize.addAndGet(2);
        int maxLimit = Runtime.getRuntime().availableProcessors() * 2;
        if (newCorePoolSize <= maxLimit) {
            threadPool.setCorePoolSize(newCorePoolSize);
            threadPool.setMaximumPoolSize(newMaximumPoolSize);
            logger.info("线程池大小增加: 核心线程数={} 最大线程数={}", newCorePoolSize, newMaximumPoolSize);
        } else {
            currentCorePoolSize.decrementAndGet();
            currentMaximumPoolSize.addAndGet(-2);
            logger.warn("线程池已达最大限制");
        }
    }

    public void reduceThreadPoolSize() {
        int currentCore = currentCorePoolSize.get();
        if (currentCore > 1) {
            int newCorePoolSize = currentCorePoolSize.decrementAndGet();
            int newMaximumPoolSize = currentMaximumPoolSize.addAndGet(-2);
            threadPool.setCorePoolSize(newCorePoolSize);
            threadPool.setMaximumPoolSize(Math.max(newCorePoolSize, newMaximumPoolSize));
            logger.info("线程池大小减少: 核心线程数={} 最大线程数={}", newCorePoolSize, newMaximumPoolSize);
        } else {
            logger.warn("无法减少线程池大小，已达到最小值");
        }
    }

    public int getThreadPoolSize() {
        return threadPool.getPoolSize();
    }

    public void addMessage(String username, Message message) {
        try {
            if (messageQueue.size() >= MAX_QUEUE_SIZE) {
                logger.warn("消息队列已满，丢弃旧消息");
                messageQueue.poll();
            }
            messageQueue.put(new MailMessage(username, message));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("添加消息到队列时出错", e);
        }
    }

    private void processMessages() {
        while (running) {
            try {
                MailMessage mailMessage = messageQueue.poll(1, TimeUnit.SECONDS);
                if (mailMessage != null) {
                    activeThreads.incrementAndGet();
                    processingMessages.incrementAndGet();
                    threadPool.execute(() -> {
                        try {
                            processSingleMessage(mailMessage);
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        } finally {
                            activeThreads.decrementAndGet();
                            processingMessages.decrementAndGet();
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("处理消息时出错", e);
            }
        }
    }

    private void processSingleMessage(MailMessage mailMessage) throws MessagingException {
        long startTime = System.nanoTime();
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                totalMessages.incrementAndGet();
                logger.info("处理账户 '{}' 的新邮件: {} (重试次数: {})", mailMessage.getUsername(), mailMessage.getMessage().getSubject(), retryCount);
                processMessage(mailMessage);
                processedMessages.incrementAndGet();
                success = true;
            } catch (MessagingException | RuntimeException e) {
                retryCount++;
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    retryMessages.incrementAndGet();
                    long retryStart = System.nanoTime();
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    totalRetryTime.addAndGet(System.nanoTime() - retryStart);
                    logger.warn("处理邮件失败，尝试重试 {} 次: {}", retryCount, e.getMessage());
                } else {
                    failedMessages.incrementAndGet();
                    logger.error("处理邮件失败，达到最大重试次数: {}", e.getMessage());
                }
            }
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        totalProcessingTime.addAndGet(duration);
        maxProcessingTime.accumulateAndGet(duration, Math::max);
        minProcessingTime.accumulateAndGet(duration, Math::min);
        if (success) {
            logger.info("成功处理邮件: {}", mailMessage.getMessage().getSubject());
        } else {
            logger.error("最终处理邮件失败: {}", mailMessage.getMessage().getSubject());
        }
    }

    private void processMessage(MailMessage mailMessage) {
        long startTime = System.nanoTime();
        try {
            Message message = mailMessage.getMessage();
            String subject = message.getSubject();
            String from = (message.getFrom() != null && message.getFrom().length > 0) ? message.getFrom()[0].toString() : "未知";
            StringBuilder contentBuilder = new StringBuilder();
            Object content = message.getContent();
            if (content instanceof String) {
                contentBuilder.append((String) content);
            } else if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/plain")) {
                        Object bodyContent = bodyPart.getContent();
                        if (bodyContent instanceof String) {
                            contentBuilder.append((String) bodyContent);
                        } else if (bodyContent instanceof InputStream) {
                            try (InputStream is = (InputStream) bodyContent) {
                                contentBuilder.append(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            } else if (content instanceof InputStream) {
                try (InputStream is = (InputStream) content) {
                    contentBuilder.append(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            String mail_content = contentBuilder.toString().trim();
            logger.info("处理邮件: {} 来自: {}", subject, from);
            logger.debug("邮件内容: {}", mail_content);
            // TODO: 实现具体的邮件处理业务逻辑
            mailMessage.setProcessed(true);
            long endTime = System.nanoTime();
            mailMessage.setProcessingTime((endTime - startTime) / 1_000_000); // ms
        } catch (MessagingException | IOException e) {
            logger.error("处理邮件时发生错误: {}", e.getMessage(), e);
            mailMessage.setProcessed(false);
            mailMessage.setErrorMessage(e.getMessage());
            mailMessage.setProcessingTime((System.nanoTime() - startTime) / 1_000_000);
            throw new RuntimeException("邮件处理失败: " + e.getMessage(), e);
        } finally {
            long processingTime = mailMessage.getProcessingTime();
            totalProcessingTime.addAndGet(processingTime);
            maxProcessingTime.accumulateAndGet(processingTime, Math::max);
            minProcessingTime.accumulateAndGet(processingTime, Math::min);
        }
    }

    public int getActiveThreads() {
        return activeThreads.get();
    }

    public int getQueueSize() {
        return messageQueue.size();
    }

    public int getProcessingMessages() {
        return processingMessages.get();
    }

    public int getThreadPoolActiveThreads() {
        return threadPool.getActiveCount();
    }

    public int getThreadPoolQueueSize() {
        return threadPool.getQueue().size();
    }

    public int getTotalMessages() {
        return totalMessages.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }

    public int getFailedMessages() {
        return failedMessages.get();
    }

    public int getRetryMessages() {
        return retryMessages.get();
    }

    public long getTotalRetryTime() {
        return totalRetryTime.get();
    }

    public double getRetrySuccessRate() {
        int total = totalMessages.get();
        return total > 0 ? ((double) (total - failedMessages.get()) / total) * 100 : 0;
    }

    public long getAverageProcessingTime() {
        long total = totalMessages.get();
        return total > 0 ? totalProcessingTime.get() / total : 0;
    }
    public long getMaxProcessingTime() {
        return maxProcessingTime.get();
    }
    public long getMinProcessingTime() {
        return minProcessingTime.get() == Long.MAX_VALUE ? 0 : minProcessingTime.get();
    }

    public boolean isHealthy() {
        return !running || (
            activeThreads.get() > 0 &&
            !threadPool.isShutdown() &&
            !threadPool.isTerminated()
        );
    }

    public String getHealthStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("MailMessageProcessor Health Status:\n");
        sb.append("- Active: ").append(running).append("\n");
        sb.append("- Active Threads: ").append(activeThreads.get()).append("\n");
        sb.append("- Processing Messages: ").append(processingMessages.get()).append("\n");
        sb.append("- Queue Size: ").append(messageQueue.size()).append("\n");
        sb.append("- ThreadPool Active Threads: ").append(threadPool.getActiveCount()).append("\n");
        sb.append("- ThreadPool Queue Size: ").append(threadPool.getQueue().size()).append("\n");
        sb.append("- Total Messages: ").append(totalMessages.get()).append("\n");
        sb.append("- Processed Messages: ").append(processedMessages.get()).append("\n");
        sb.append("- Failed Messages: ").append(failedMessages.get()).append("\n");
        sb.append("- Average Processing Time: ").append(getAverageProcessingTime()).append("ns\n");
        sb.append("- Max Processing Time: ").append(getMaxProcessingTime()).append("ns\n");
        sb.append("- Min Processing Time: ").append(getMinProcessingTime()).append("ns\n");
        return sb.toString();
    }

    public void shutdown() {
        running = false;
        if (processorThread != null) {
            processorThread.interrupt();
            try {
                processorThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
