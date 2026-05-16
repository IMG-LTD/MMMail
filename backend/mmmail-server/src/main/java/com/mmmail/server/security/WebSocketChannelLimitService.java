package com.mmmail.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WebSocketChannelLimitService {

    private final ConcurrentMap<String, Set<String>> channelsBySession = new ConcurrentHashMap<>();
    private final int maxChannelsPerSession;
    private final long retryAfterMs;

    public WebSocketChannelLimitService(
            @Value("${mmmail.websocket.subscription.max-channels-per-session:32}") int maxChannelsPerSession,
            @Value("${mmmail.websocket.subscription.retry-after-ms:1000}") long retryAfterMs
    ) {
        this.maxChannelsPerSession = requirePositive(
                "mmmail.websocket.subscription.max-channels-per-session",
                maxChannelsPerSession
        );
        this.retryAfterMs = requirePositive("mmmail.websocket.subscription.retry-after-ms", retryAfterMs);
    }

    public ChannelDecision subscribe(String sessionId, String channel) {
        String normalizedSessionId = requireText("websocket session id", sessionId);
        String normalizedChannel = requireText("websocket channel", channel);
        Set<String> channels = channelsBySession.computeIfAbsent(
                normalizedSessionId,
                ignored -> ConcurrentHashMap.newKeySet()
        );
        if (channels.contains(normalizedChannel)) {
            return ChannelDecision.allowed(retryAfterMs);
        }
        synchronized (channels) {
            return subscribeWithinLock(channels, normalizedChannel);
        }
    }

    public void release(String sessionId) {
        channelsBySession.remove(requireText("websocket session id", sessionId));
    }

    private ChannelDecision subscribeWithinLock(Set<String> channels, String channel) {
        if (channels.size() >= maxChannelsPerSession) {
            return ChannelDecision.denied(retryAfterMs);
        }
        channels.add(channel);
        return ChannelDecision.allowed(retryAfterMs);
    }

    private String requireText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
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

    public record ChannelDecision(boolean allowed, long retryAfterMs) {

        private static ChannelDecision allowed(long retryAfterMs) {
            return new ChannelDecision(true, retryAfterMs);
        }

        private static ChannelDecision denied(long retryAfterMs) {
            return new ChannelDecision(false, retryAfterMs);
        }
    }
}
