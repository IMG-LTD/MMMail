package com.mmmail.server.outbox;

import com.mmmail.platform.event.PlatformEventType;
import com.mmmail.platform.outbox.OutboxDispatchResult;
import com.mmmail.platform.outbox.OutboxDispatcher;
import com.mmmail.platform.outbox.OutboxEventRecord;
import com.mmmail.platform.outbox.OutboxEventStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InProcessOutboxDispatcher implements OutboxDispatcher {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int MAX_BATCH_SIZE = 100;
    private static final int ERROR_MAX_LENGTH = 512;
    private static final String METRIC_DISPATCH_TOTAL = "mmmail.outbox.events.dispatched.total";
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(1);

    private final PlatformOutboxEventMapper mapper;
    private final MeterRegistry meterRegistry;
    private final DispatcherOptions options;

    @Autowired
    public InProcessOutboxDispatcher(PlatformOutboxEventMapper mapper, MeterRegistry meterRegistry) {
        this(mapper, meterRegistry, DispatcherOptions.defaultOptions());
    }

    public InProcessOutboxDispatcher(
            PlatformOutboxEventMapper mapper,
            MeterRegistry meterRegistry,
            DispatcherOptions options
    ) {
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        this.options = options;
    }

    @Override
    @Transactional
    public OutboxDispatchResult dispatchDue(int limit, LocalDateTime now) {
        List<PlatformOutboxEvent> dueEvents = mapper.findDue(now, safeLimit(limit));
        DispatchCounters counters = new DispatchCounters(dueEvents.size());
        for (PlatformOutboxEvent entity : dueEvents) {
            dispatchOne(entity, now, counters);
        }
        return counters.toResult();
    }

    private void dispatchOne(PlatformOutboxEvent entity, LocalDateTime now, DispatchCounters counters) {
        OutboxEventRecord record = toRecord(entity);
        try {
            options.handler().handle(record);
            markPublished(entity, now);
            counters.recordPublished();
            counter(record, OutboxEventStatus.PUBLISHED).increment();
        } catch (RuntimeException ex) {
            markFailedOrDeadLetter(entity, now, ex);
            counters.recordFailure(entity.getStatus());
            counter(record, OutboxEventStatus.valueOf(entity.getStatus())).increment();
        }
    }

    private void markPublished(PlatformOutboxEvent entity, LocalDateTime now) {
        entity.setStatus(OutboxEventStatus.PUBLISHED.name());
        entity.setNextAttemptAt(null);
        entity.setLastError(null);
        entity.setUpdatedAt(now);
        entity.setPublishedAt(now);
        mapper.updateById(entity);
    }

    private void markFailedOrDeadLetter(PlatformOutboxEvent entity, LocalDateTime now, RuntimeException ex) {
        int attempts = nextAttempts(entity);
        entity.setAttempts(attempts);
        entity.setLastError(trimError(ex.getMessage()));
        entity.setUpdatedAt(now);
        if (attempts >= options.maxAttempts()) {
            entity.setStatus(OutboxEventStatus.DEAD_LETTER.name());
            entity.setNextAttemptAt(null);
        } else {
            entity.setStatus(OutboxEventStatus.FAILED.name());
            entity.setNextAttemptAt(now.plus(options.retryDelay()));
        }
        mapper.updateById(entity);
    }

    private static OutboxEventRecord toRecord(PlatformOutboxEvent entity) {
        return new OutboxEventRecord(
                entity.getId(),
                PlatformEventType.fromEventName(entity.getEventType()),
                entity.getOwnerModule(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getRequestId(),
                entity.getTraceId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getPayloadJson(),
                entity.getIdempotencyKey(),
                OutboxEventStatus.valueOf(entity.getStatus()),
                currentAttempts(entity),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt()
        );
    }

    private static int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_BATCH_SIZE));
    }

    private static int nextAttempts(PlatformOutboxEvent entity) {
        return currentAttempts(entity) + 1;
    }

    private static int currentAttempts(PlatformOutboxEvent entity) {
        return entity.getAttempts() == null ? 0 : entity.getAttempts();
    }

    private static String trimError(String message) {
        if (message == null || message.length() <= ERROR_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MAX_LENGTH);
    }

    private Counter counter(OutboxEventRecord record, OutboxEventStatus status) {
        return Counter.builder(METRIC_DISPATCH_TOTAL)
                .tag("event", record.eventName())
                .tag("module", record.ownerModule())
                .tag("status", status.name())
                .register(meterRegistry);
    }

    public record DispatcherOptions(
            PlatformOutboxHandler handler,
            int maxAttempts,
            Duration retryDelay
    ) {
        public DispatcherOptions {
            if (handler == null) {
                throw new IllegalArgumentException("handler is required");
            }
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be positive");
            }
            if (retryDelay == null) {
                throw new IllegalArgumentException("retryDelay is required");
            }
        }

        public static DispatcherOptions defaultOptions() {
            return new DispatcherOptions(eventRecord -> {
            }, DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_DELAY);
        }
    }

    private static final class DispatchCounters {

        private final int scanned;
        private int published;
        private int failed;
        private int deadLettered;

        private DispatchCounters(int scanned) {
            this.scanned = scanned;
        }

        private void recordPublished() {
            published++;
        }

        private void recordFailure(String status) {
            if (OutboxEventStatus.DEAD_LETTER.name().equals(status)) {
                deadLettered++;
            } else {
                failed++;
            }
        }

        private OutboxDispatchResult toResult() {
            return new OutboxDispatchResult(scanned, published, failed, deadLettered);
        }
    }
}
