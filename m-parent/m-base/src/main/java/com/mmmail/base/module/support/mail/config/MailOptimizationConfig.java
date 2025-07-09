package com.mmmail.base.module.support.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mail.optimization")
@Data
public class MailOptimizationConfig {
    /**
     * 连接池配置
     */
    private ConnectionPool connectionPool = new ConnectionPool();
    
    /**
     * 消息队列配置
     */
    private MessageQueue messageQueue = new MessageQueue();
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    /**
     * 压缩配置
     */
    private Compression compression = new Compression();
    
    @Data
    public static class ConnectionPool {
        private int maxTotal = 100; // 最大连接数
        private int maxIdle = 50; // 最大空闲连接数
        private int minIdle = 10; // 最小空闲连接数
        private long maxWaitMillis = 10000; // 获取连接最大等待时间
        private boolean testOnBorrow = true; // 获取连接时测试有效性
        private long timeBetweenEvictionRunsMillis = 300000; // 300秒检查空闲连接
        private long minEvictableIdleTimeMillis = 600000; // 600秒后回收空闲连接
    }
    
    @Data
    public static class MessageQueue {
        private int maxSize = 1000; // 队列最大大小
        private int batchSize = 100; // 批处理大小
        private int threadPoolSize = 10; // 线程池大小
        private long retryDelay = 30000; // 重试延迟
        private int maxRetries = 3; // 最大重试次数
    }
    
    @Data
    public static class Cache {
        private long ttl = 3600000; // 缓存过期时间（毫秒）
        private int maxSize = 1000; // 缓存最大大小
        private boolean enabled = true; // 是否启用缓存
        private long cleanupInterval = 3600000; // 清理间隔（毫秒）
    }
    
    @Data
    public static class Compression {
        private boolean enabled = true; // 是否启用压缩
        private double threshold = 1024.0; // 压缩阈值（字节）
        private String algorithm = "bzip2"; // 压缩算法
        private double compressionRatio = 0.5; // 压缩率目标
    }
}
