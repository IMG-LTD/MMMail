package com.mmmail.base.module.support.mail.controller;

import com.mmmail.base.module.support.mail.service.MailMonitor;
import com.mmmail.base.module.support.mail.service.ImapConnectionPool;
import com.mmmail.base.module.support.mail.service.MailMessageProcessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * 邮件监控接口
 * Mail monitor controller
 */
@Tag(name = "邮件监控接口 | Mail Monitor API")
@RestController
@RequestMapping("/api/mail/monitor")
public class MailMonitorController {

    @Autowired
    private MailMonitor mailMonitor;

    /**
     * 获取邮件系统资源监控信息
     * Get mail system resource monitor info
     * @return 监控信息map | monitor info map
     */
    @Operation(summary = "获取邮件系统资源监控信息 | Get mail system resource monitor info", description = "返回连接数、线程数、队列长度、已处理消息数、平均处理时长等核心指标 | Returns connection count, thread count, queue size, processed message count, average processing time, etc.")
    @GetMapping("/info")
    public Map<String, Object> getMonitorInfo() {
        Map<String, Object> info = new HashMap<>();
        ImapConnectionPool pool = mailMonitor.getConnectionPool();
        MailMessageProcessor processor = mailMonitor.getMessageProcessor();
        int totalConnections = pool.getNumActive() + pool.getNumIdle();
        int activeThreads = processor.getActiveThreads();
        int queueSize = processor.getQueueSize();
        int processedMessages = processor.getProcessedMessages();
        long avgProcessingTime = processor.getAverageProcessingTime();
        info.put("activeConnections", pool.getNumActive());
        info.put("totalConnections", totalConnections);
        info.put("activeThreads", activeThreads);
        info.put("queueSize", queueSize);
        info.put("processedMessages", processedMessages);
        info.put("avgProcessingTime", avgProcessingTime);
        return info;
    }
} 