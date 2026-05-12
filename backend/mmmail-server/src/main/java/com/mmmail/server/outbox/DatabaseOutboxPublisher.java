package com.mmmail.server.outbox;

import com.mmmail.platform.event.PlatformEvent;
import com.mmmail.platform.event.PlatformEventMetadata;
import com.mmmail.platform.outbox.OutboxEventStatus;
import com.mmmail.platform.outbox.OutboxPublishRequest;
import com.mmmail.platform.outbox.OutboxPublishResult;
import com.mmmail.platform.outbox.OutboxPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class DatabaseOutboxPublisher implements OutboxPublisher {

    private static final String METRIC_PUBLISHED_TOTAL = "mmmail.outbox.events.published.total";

    private final PlatformOutboxEventMapper mapper;
    private final MeterRegistry meterRegistry;

    public DatabaseOutboxPublisher(PlatformOutboxEventMapper mapper, MeterRegistry meterRegistry) {
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public OutboxPublishResult publish(OutboxPublishRequest request) {
        PlatformEvent event = request.event();
        PlatformOutboxEvent existing = mapper.findByIdempotencyKey(event.idempotencyKey());
        if (existing != null) {
            return duplicateResult(existing, event);
        }
        PlatformOutboxEvent entity = toEntity(event, LocalDateTime.now());
        mapper.insert(entity);
        counter(event.eventName(), event.ownerModule(), entity.getStatus()).increment();
        return new OutboxPublishResult(entity.getId(), OutboxEventStatus.PENDING, false);
    }

    private OutboxPublishResult duplicateResult(PlatformOutboxEvent existing, PlatformEvent event) {
        if (!sameEvent(existing, event)) {
            throw new IllegalStateException("idempotency key already belongs to a different event");
        }
        OutboxEventStatus status = OutboxEventStatus.valueOf(existing.getStatus());
        counter(event.eventName(), event.ownerModule(), existing.getStatus()).increment();
        return new OutboxPublishResult(existing.getId(), status, true);
    }

    private static PlatformOutboxEvent toEntity(PlatformEvent event, LocalDateTime now) {
        PlatformEventMetadata metadata = event.metadata();
        PlatformOutboxEvent entity = new PlatformOutboxEvent();
        entity.setEventType(event.eventName());
        entity.setOwnerModule(event.ownerModule());
        entity.setTenantId(metadata.tenantId());
        entity.setUserId(metadata.userId());
        entity.setRequestId(metadata.requestId());
        entity.setTraceId(metadata.traceId());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setPayloadJson(event.payloadJson());
        entity.setIdempotencyKey(event.idempotencyKey());
        entity.setStatus(OutboxEventStatus.PENDING.name());
        entity.setAttempts(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static boolean sameEvent(PlatformOutboxEvent existing, PlatformEvent event) {
        return Objects.equals(existing.getEventType(), event.eventName())
                && Objects.equals(existing.getAggregateType(), event.aggregateType())
                && Objects.equals(existing.getAggregateId(), event.aggregateId())
                && Objects.equals(existing.getPayloadJson(), event.payloadJson());
    }

    private Counter counter(String eventName, String ownerModule, String status) {
        return Counter.builder(METRIC_PUBLISHED_TOTAL)
                .tag("event", eventName)
                .tag("module", ownerModule)
                .tag("status", status)
                .register(meterRegistry);
    }
}
