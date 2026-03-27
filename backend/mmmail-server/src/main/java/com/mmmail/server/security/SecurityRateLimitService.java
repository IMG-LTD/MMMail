package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityRateLimitService {

    private static final String LOGIN_SCOPE = "login";
    private static final String CLIENT_ERRORS_SCOPE = "client-errors";
    private static final String LOGIN_LIMIT_MESSAGE = "Too many login attempts. Try again later.";
    private static final String CLIENT_ERRORS_LIMIT_MESSAGE = "Client error reporting is temporarily rate limited";

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    private final long loginWindowMs;
    private final int loginMaxAttempts;
    private final long clientErrorsWindowMs;
    private final int clientErrorsMaxEvents;

    public SecurityRateLimitService(
            MeterRegistry meterRegistry,
            @Value("${mmmail.security.rate-limit.login.window-seconds:300}") long loginWindowSeconds,
            @Value("${mmmail.security.rate-limit.login.max-attempts:10}") int loginMaxAttempts,
            @Value("${mmmail.security.rate-limit.client-errors.window-seconds:60}") long clientErrorsWindowSeconds,
            @Value("${mmmail.security.rate-limit.client-errors.max-events:30}") int clientErrorsMaxEvents
    ) {
        this.meterRegistry = meterRegistry;
        this.loginWindowMs = toWindowMs(loginWindowSeconds);
        this.loginMaxAttempts = Math.max(1, loginMaxAttempts);
        this.clientErrorsWindowMs = toWindowMs(clientErrorsWindowSeconds);
        this.clientErrorsMaxEvents = Math.max(1, clientErrorsMaxEvents);
    }

    public void ensureLoginAllowed(String email, String ipAddress) {
        ensureAllowed(
                LOGIN_SCOPE,
                loginKey(email, ipAddress),
                loginWindowMs,
                loginMaxAttempts,
                LOGIN_LIMIT_MESSAGE
        );
    }

    public void recordLoginFailure(String email, String ipAddress) {
        increment(loginKey(email, ipAddress), loginWindowMs);
    }

    public void resetLoginFailures(String email, String ipAddress) {
        counters.remove(loginKey(email, ipAddress));
    }

    public void recordClientErrorEvent(Long userId, Long sessionId, String ipAddress) {
        int nextCount = increment(clientErrorsKey(userId, sessionId, ipAddress), clientErrorsWindowMs);
        if (nextCount <= clientErrorsMaxEvents) {
            return;
        }
        recordLimited(CLIENT_ERRORS_SCOPE);
        throw new BizException(ErrorCode.RATE_LIMITED, CLIENT_ERRORS_LIMIT_MESSAGE);
    }

    private void ensureAllowed(String scope, String key, long windowMs, int maxCount, String message) {
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.get(key);
        if (counter == null) {
            return;
        }
        if (counter.isExpired(now, windowMs)) {
            counters.remove(key, counter);
            return;
        }
        if (counter.count() < maxCount) {
            return;
        }
        recordLimited(scope);
        throw new BizException(ErrorCode.RATE_LIMITED, message);
    }

    private int increment(String key, long windowMs) {
        long now = System.currentTimeMillis();
        WindowCounter updated = counters.compute(key, (ignored, current) -> WindowCounter.next(current, now, windowMs));
        pruneExpired(now);
        return updated.count();
    }

    private void pruneExpired(long now) {
        if (counters.size() < 1024) {
            return;
        }
        long maxWindowMs = Math.max(loginWindowMs, clientErrorsWindowMs);
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now, maxWindowMs));
    }

    private void recordLimited(String scope) {
        Counter.builder("mmmail.security.rate_limited.total")
                .tag("scope", scope)
                .register(meterRegistry)
                .increment();
    }

    private String loginKey(String email, String ipAddress) {
        return LOGIN_SCOPE + ":" + normalize(email) + ":" + normalizeIp(ipAddress);
    }

    private String clientErrorsKey(Long userId, Long sessionId, String ipAddress) {
        if (sessionId != null) {
            return CLIENT_ERRORS_SCOPE + ":session:" + sessionId;
        }
        if (userId != null) {
            return CLIENT_ERRORS_SCOPE + ":user:" + userId;
        }
        return CLIENT_ERRORS_SCOPE + ":ip:" + normalizeIp(ipAddress);
    }

    private long toWindowMs(long windowSeconds) {
        return Math.max(1L, windowSeconds) * 1000L;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "unknown";
    }

    private String normalizeIp(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress.trim() : "0.0.0.0";
    }

    private record WindowCounter(long windowStartedAtMs, int count) {

        private static WindowCounter next(WindowCounter current, long now, long windowMs) {
            if (current == null || current.isExpired(now, windowMs)) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(current.windowStartedAtMs(), current.count() + 1);
        }

        private boolean isExpired(long now, long windowMs) {
            return now - windowStartedAtMs >= windowMs;
        }
    }
}
