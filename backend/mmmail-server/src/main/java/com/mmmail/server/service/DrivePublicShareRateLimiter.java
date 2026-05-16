package com.mmmail.server.service;

import com.mmmail.server.observability.RuntimeTraceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DrivePublicShareRateLimiter {

    private static final int DEFAULT_WINDOW_SECONDS = 60;
    private static final int DEFAULT_MAX_REQUESTS = 30;
    private static final int MAX_COUNTERS_BEFORE_CLEANUP = 10_000;
    private static final long FIRST_COUNTER_VALUE = 1L;
    private static final long COUNTER_EXPIRY_WINDOW_MULTIPLIER = 2L;
    private static final String DEFAULT_REDIS_KEY_PREFIX = "mmmail:drive:share-rate";
    private static final String UNKNOWN_IP = "unknown";
    private static final String REDIS_SPAN = "mmmail.redis.operation";

    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final RuntimeTraceService runtimeTraceService;
    private final Map<String, PublicRateCounter> publicRateCounterMap = new ConcurrentHashMap<>();
    @Value("${mmmail.drive.public-share-rate-limit.window-seconds:60}")
    private Integer windowSeconds;
    @Value("${mmmail.drive.public-share-rate-limit.max-requests:30}")
    private Integer maxRequests;
    @Value("${mmmail.drive.public-share-rate-limit.redis-key-prefix:mmmail:drive:share-rate}")
    private String redisKeyPrefix;

    public DrivePublicShareRateLimiter(
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            RuntimeTraceService runtimeTraceService
    ) {
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
        this.runtimeTraceService = runtimeTraceService;
    }

    public boolean isLimited(String token, String ipAddress, String action) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        RateLimitRequest request = new RateLimitRequest(
                token,
                normalizeIp(ipAddress),
                action,
                effectiveWindowSeconds(),
                effectiveMaxRequests()
        );
        Boolean redisLimited = isLimitedByRedis(request);
        if (redisLimited != null) {
            return redisLimited;
        }
        return isLimitedInMemory(request);
    }

    private Boolean isLimitedByRedis(RateLimitRequest request) {
        StringRedisTemplate redisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            return runtimeTraceService.observe(REDIS_SPAN, Map.of(
                    "component", "redis",
                    "operation", "public_share_rate_limit",
                    "action", request.action()
            ), () -> incrementRedisCounter(redisTemplate, request));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean incrementRedisCounter(StringRedisTemplate redisTemplate, RateLimitRequest request) {
        String key = redisKey(request);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return null;
        }
        if (count == FIRST_COUNTER_VALUE) {
            redisTemplate.expire(key, Duration.ofSeconds(request.windowSeconds()));
        }
        return count > request.maxRequests();
    }

    private boolean isLimitedInMemory(RateLimitRequest request) {
        long nowSeconds = Instant.now().getEpochSecond();
        AtomicBoolean limited = new AtomicBoolean(false);
        publicRateCounterMap.compute(counterKey(request), (key, existing) -> {
            PublicRateCounter counter = existing == null ? new PublicRateCounter(nowSeconds) : existing;
            if ((nowSeconds - counter.windowStartSeconds()) >= request.windowSeconds()) {
                counter.reset(nowSeconds);
            }
            int current = counter.count().incrementAndGet();
            counter.setLastSeenSeconds(nowSeconds);
            limited.set(current > request.maxRequests());
            return counter;
        });
        cleanupCounterMapIfNeeded(nowSeconds, request.windowSeconds());
        return limited.get();
    }

    private void cleanupCounterMapIfNeeded(long nowSeconds, int windowSeconds) {
        if (publicRateCounterMap.size() < MAX_COUNTERS_BEFORE_CLEANUP) {
            return;
        }
        long expireBefore = nowSeconds - (windowSeconds * COUNTER_EXPIRY_WINDOW_MULTIPLIER);
        publicRateCounterMap.entrySet().removeIf(entry -> entry.getValue().lastSeenSeconds() < expireBefore);
    }

    private String redisKey(RateLimitRequest request) {
        return effectiveRedisKeyPrefix() + ":" + counterKey(request);
    }

    private String counterKey(RateLimitRequest request) {
        return request.token() + ":" + request.normalizedIp() + ":" + request.action();
    }

    private String normalizeIp(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress.trim() : UNKNOWN_IP;
    }

    private int effectiveWindowSeconds() {
        if (windowSeconds == null || windowSeconds <= 0) {
            return DEFAULT_WINDOW_SECONDS;
        }
        return windowSeconds;
    }

    private int effectiveMaxRequests() {
        if (maxRequests == null || maxRequests <= 0) {
            return DEFAULT_MAX_REQUESTS;
        }
        return maxRequests;
    }

    private String effectiveRedisKeyPrefix() {
        if (!StringUtils.hasText(redisKeyPrefix)) {
            return DEFAULT_REDIS_KEY_PREFIX;
        }
        return redisKeyPrefix.trim();
    }

    private record RateLimitRequest(String token, String normalizedIp, String action, int windowSeconds, int maxRequests) {
    }

    private static final class PublicRateCounter {
        private long windowStartSeconds;
        private long lastSeenSeconds;
        private final AtomicInteger count;

        private PublicRateCounter(long nowSeconds) {
            this.windowStartSeconds = nowSeconds;
            this.lastSeenSeconds = nowSeconds;
            this.count = new AtomicInteger(0);
        }

        private void reset(long nowSeconds) {
            this.windowStartSeconds = nowSeconds;
            this.lastSeenSeconds = nowSeconds;
            this.count.set(0);
        }

        private long windowStartSeconds() {
            return windowStartSeconds;
        }

        private long lastSeenSeconds() {
            return lastSeenSeconds;
        }

        private void setLastSeenSeconds(long nowSeconds) {
            this.lastSeenSeconds = nowSeconds;
        }

        private AtomicInteger count() {
            return count;
        }
    }
}
