package com.mmmail.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketConnectionLimitService {

    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();
    private final int maxConnections;
    private final long retryAfterMs;

    public WebSocketConnectionLimitService(
            @Value("${mmmail.websocket.connection.max-active:10000}") int maxConnections,
            @Value("${mmmail.websocket.connection.retry-after-ms:1000}") long retryAfterMs
    ) {
        this.maxConnections = requirePositive("mmmail.websocket.connection.max-active", maxConnections);
        this.retryAfterMs = requirePositive("mmmail.websocket.connection.retry-after-ms", retryAfterMs);
    }

    public ConnectionDecision open(String sessionId) {
        String normalizedSessionId = requireSessionId(sessionId);
        if (activeSessions.contains(normalizedSessionId)) {
            return ConnectionDecision.allowed(retryAfterMs);
        }
        synchronized (activeSessions) {
            return openWithinLock(normalizedSessionId);
        }
    }

    public void release(String sessionId) {
        activeSessions.remove(requireSessionId(sessionId));
    }

    private ConnectionDecision openWithinLock(String sessionId) {
        if (activeSessions.size() >= maxConnections) {
            return ConnectionDecision.denied(retryAfterMs);
        }
        activeSessions.add(sessionId);
        return ConnectionDecision.allowed(retryAfterMs);
    }

    private String requireSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("websocket session id must not be blank");
        }
        return sessionId.trim();
    }

    private int requirePositive(String propertyName, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
        return value;
    }

    private long requirePositive(String propertyName, long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
        return value;
    }

    public record ConnectionDecision(boolean allowed, long retryAfterMs) {

        private static ConnectionDecision allowed(long retryAfterMs) {
            return new ConnectionDecision(true, retryAfterMs);
        }

        private static ConnectionDecision denied(long retryAfterMs) {
            return new ConnectionDecision(false, retryAfterMs);
        }
    }
}
