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
    private static final String MAIL_SEND_SCOPE = "mail-send";
    private static final String WEB_PUSH_TEST_SCOPE = "web-push-test";
    private static final String COMMAND_RUN_SCOPE = "command-run";
    private static final String LOGIN_LIMIT_MESSAGE = "Too many login attempts. Try again later.";
    private static final String CLIENT_ERRORS_LIMIT_MESSAGE = "Client error reporting is temporarily rate limited";
    private static final String MAIL_SEND_LIMIT_MESSAGE = "Mail sending is temporarily rate limited";
    private static final String WEB_PUSH_TEST_LIMIT_MESSAGE = "Web Push test delivery is temporarily rate limited";
    private static final String COMMAND_RUN_LIMIT_MESSAGE = "Command execution is temporarily rate limited";

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    private final long loginWindowMs;
    private final int loginMaxAttempts;
    private final long clientErrorsWindowMs;
    private final int clientErrorsMaxEvents;
    private final RateLimitPolicy mailSendPolicy;
    private final RateLimitPolicy webPushTestPolicy;
    private final RateLimitPolicy commandRunPolicy;

    public SecurityRateLimitService(
            MeterRegistry meterRegistry,
            @Value("${mmmail.security.rate-limit.login.window-seconds:300}") long loginWindowSeconds,
            @Value("${mmmail.security.rate-limit.login.max-attempts:10}") int loginMaxAttempts,
            @Value("${mmmail.security.rate-limit.client-errors.window-seconds:60}") long clientErrorsWindowSeconds,
            @Value("${mmmail.security.rate-limit.client-errors.max-events:30}") int clientErrorsMaxEvents,
            @Value("${mmmail.security.rate-limit.mail-send.window-seconds:60}") long mailSendWindowSeconds,
            @Value("${mmmail.security.rate-limit.mail-send.max-events:30}") int mailSendMaxEvents,
            @Value("${mmmail.security.rate-limit.web-push-test.window-seconds:60}") long webPushTestWindowSeconds,
            @Value("${mmmail.security.rate-limit.web-push-test.max-events:5}") int webPushTestMaxEvents,
            @Value("${mmmail.security.rate-limit.command-run.window-seconds:60}") long commandRunWindowSeconds,
            @Value("${mmmail.security.rate-limit.command-run.max-events:10}") int commandRunMaxEvents
    ) {
        this.meterRegistry = meterRegistry;
        this.loginWindowMs = toWindowMs(loginWindowSeconds);
        this.loginMaxAttempts = Math.max(1, loginMaxAttempts);
        this.clientErrorsWindowMs = toWindowMs(clientErrorsWindowSeconds);
        this.clientErrorsMaxEvents = Math.max(1, clientErrorsMaxEvents);
        this.mailSendPolicy = new RateLimitPolicy(toWindowMs(mailSendWindowSeconds), Math.max(1, mailSendMaxEvents));
        this.webPushTestPolicy = new RateLimitPolicy(toWindowMs(webPushTestWindowSeconds), Math.max(1, webPushTestMaxEvents));
        this.commandRunPolicy = new RateLimitPolicy(toWindowMs(commandRunWindowSeconds), Math.max(1, commandRunMaxEvents));
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

    public void recordMailSendAttempt(Long userId, String ipAddress) {
        recordLimitedAction(new RateLimitedAction(
                MAIL_SEND_SCOPE,
                actionKey(MAIL_SEND_SCOPE, userId, ipAddress),
                mailSendPolicy,
                MAIL_SEND_LIMIT_MESSAGE
        ));
    }

    public void recordWebPushTestAttempt(Long userId, String ipAddress) {
        recordLimitedAction(new RateLimitedAction(
                WEB_PUSH_TEST_SCOPE,
                actionKey(WEB_PUSH_TEST_SCOPE, userId, ipAddress),
                webPushTestPolicy,
                WEB_PUSH_TEST_LIMIT_MESSAGE
        ));
    }

    public void recordCommandRunAttempt(Long userId, String ipAddress) {
        recordLimitedAction(new RateLimitedAction(
                COMMAND_RUN_SCOPE,
                actionKey(COMMAND_RUN_SCOPE, userId, ipAddress),
                commandRunPolicy,
                COMMAND_RUN_LIMIT_MESSAGE
        ));
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

    private void recordLimitedAction(RateLimitedAction action) {
        int nextCount = increment(action.key(), action.policy().windowMs());
        if (nextCount <= action.policy().maxEvents()) {
            return;
        }
        recordLimited(action.scope());
        throw new BizException(ErrorCode.RATE_LIMITED, action.message());
    }

    private void pruneExpired(long now) {
        if (counters.size() < 1024) {
            return;
        }
        long maxWindowMs = Math.max(Math.max(loginWindowMs, clientErrorsWindowMs), maxActionWindowMs());
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now, maxWindowMs));
    }

    private long maxActionWindowMs() {
        return Math.max(mailSendPolicy.windowMs(), Math.max(webPushTestPolicy.windowMs(), commandRunPolicy.windowMs()));
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

    private String actionKey(String scope, Long userId, String ipAddress) {
        if (userId != null) {
            return scope + ":user:" + userId;
        }
        return scope + ":ip:" + normalizeIp(ipAddress);
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

    private record RateLimitPolicy(long windowMs, int maxEvents) {
    }

    private record RateLimitedAction(String scope, String key, RateLimitPolicy policy, String message) {
    }
}
