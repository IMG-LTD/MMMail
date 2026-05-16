package com.mmmail.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WebSocketThrottleService {

    private static final int PRUNE_THRESHOLD = 1024;
    private static final long MILLIS_PER_SECOND = Duration.ofSeconds(1).toMillis();

    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final int maxMessages;
    private final long windowMs;
    private final long retryAfterMs;

    public WebSocketThrottleService(
            @Value("${mmmail.websocket.rate-limit.max-messages-per-window:100}") int maxMessages,
            @Value("${mmmail.websocket.rate-limit.window-seconds:1}") long windowSeconds,
            @Value("${mmmail.websocket.rate-limit.retry-after-ms:1000}") long retryAfterMs
    ) {
        this.maxMessages = requirePositive("mmmail.websocket.rate-limit.max-messages-per-window", maxMessages);
        this.windowMs = requirePositive("mmmail.websocket.rate-limit.window-seconds", windowSeconds) * MILLIS_PER_SECOND;
        this.retryAfterMs = requirePositive("mmmail.websocket.rate-limit.retry-after-ms", retryAfterMs);
    }

    public ThrottleDecision recordMessage(String sessionId) {
        long now = System.currentTimeMillis();
        WindowCounter updated = counters.compute(requireSessionId(sessionId), (ignored, current) -> next(current, now));
        pruneExpired(now);
        return updated.count() <= maxMessages
                ? ThrottleDecision.allowed(retryAfterMs)
                : ThrottleDecision.denied(retryAfterMs);
    }

    public void release(String sessionId) {
        counters.remove(requireSessionId(sessionId));
    }

    private WindowCounter next(WindowCounter current, long now) {
        if (current == null || current.isExpired(now, windowMs)) {
            return new WindowCounter(now, 1);
        }
        return new WindowCounter(current.windowStartedAtMs(), current.count() + 1);
    }

    private void pruneExpired(long now) {
        if (counters.size() < PRUNE_THRESHOLD) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMs));
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

    private String requireSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("websocket session id must not be blank");
        }
        return sessionId.trim();
    }

    public record ThrottleDecision(boolean allowed, long retryAfterMs) {

        private static ThrottleDecision allowed(long retryAfterMs) {
            return new ThrottleDecision(true, retryAfterMs);
        }

        private static ThrottleDecision denied(long retryAfterMs) {
            return new ThrottleDecision(false, retryAfterMs);
        }
    }

    private record WindowCounter(long windowStartedAtMs, int count) {

        private boolean isExpired(long now, long windowMs) {
            return now - windowStartedAtMs >= windowMs;
        }
    }
}
